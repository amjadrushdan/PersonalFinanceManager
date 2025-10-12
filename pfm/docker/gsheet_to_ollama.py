import os
import requests
import gspread
from google.oauth2.service_account import Credentials
from dotenv import load_dotenv
import logging
import time
import json
import re
import requests
import logging

# # ---- Check internet connectivity ----
# requests.get("https://oauth2.googleapis.com", timeout=5)
# requests.get("https://sheets.googleapis.com", timeout=5)

# # ---- Setup logging ----
# logging.basicConfig(
#     level=logging.INFO,
#     format="%(asctime)s [%(levelname)s] %(message)s",
#     datefmt="%Y-%m-%d %H:%M:%S"
# )

# ANSI color codes
COLORS = {
    "INFO": "\033[34m",    # Blue
    "WARNING": "\033[33m", # Yellow
    "ERROR": "\033[31m",   # Red
    "RESET": "\033[0m"
}

# ---- Setup colored logging ----
class ColoredFormatter(logging.Formatter):
    def format(self, record):
        levelname = record.levelname
        color = COLORS.get(levelname, COLORS["RESET"])
        record.levelname = f"{color}{levelname}{COLORS['RESET']}"
        return super().format(record)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] [%(funcName)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S"
)
logger = logging.getLogger()
for handler in logger.handlers:
    handler.setFormatter(ColoredFormatter(handler.formatter._fmt, handler.formatter.datefmt))


# ---- Load environment variables ----
load_dotenv()
SERVICE_ACCOUNT_FILE = os.getenv("GOOGLE_SERVICE_ACCOUNT_FILE")
SPREADSHEET_ID = os.getenv("GOOGLE_SHEET_ID")
SHEET_NAME = "ExtractedText"
PROMPT_FILE = "prompt.txt"

# Ollama settings
OLLAMA_URL = 'http://localhost:11434/api/generate'
MODEL_NAME = 'qwen2.5-coder:3b'


# ---- Google Sheets Setup ----
def get_gsheet():
    """Fetch data from Google Sheets."""
    global sheet 
    try:
        logging.info("Connecting to Google Sheets...")
        # Change to full access (read/write)
        scope = ["https://www.googleapis.com/auth/spreadsheets"]
        logging.info("Loading credentials...")
        creds = Credentials.from_service_account_file(SERVICE_ACCOUNT_FILE, scopes=scope)
        logging.info("Authorizing credentials...")
        gc = gspread.authorize(creds)
        logging.info("Opening spreadsheet...")
        sheet = gc.open_by_key(SPREADSHEET_ID).worksheet(SHEET_NAME)
        logging.info("Connected to Google Sheet successfully.")
    except Exception as e:
        logging.error(f"Failed to connect to Google Sheet: {e}")
        exit()

# ---- Functions ----
def get_prompt_text():
    """Load the base prompt template."""
    try:
        with open(PROMPT_FILE, "r", encoding="utf-8") as f:
            logging.info("Prompt template loaded.")
            return f.read()
    except Exception as e:
        logging.error(f"Error reading prompt file: {e}")
        return ""

def send_to_ollama(receipt_text):
    """Send receipt text to Ollama model and return the response text."""
    logging.info("Sending data to Ollama model...")
    prompt_template = get_prompt_text()
    final_prompt = f"{prompt_template}\n\n{receipt_text}"

    payload = {
        "model": MODEL_NAME,
        "prompt": final_prompt,
        "stream": False
    }

    try:
        resp = requests.post(OLLAMA_URL, json=payload, timeout=120)
        resp.raise_for_status()
        logging.info("Received response from Ollama.")
        return resp.json().get("response", "")
    except Exception as e:
        logging.error(f"Error calling Ollama: {e}")
        return None

def clean_json_text(text):
    """Remove code fences and stray markdown formatting."""
    if not text:
        return text
    # Remove ```json or ``` if present
    cleaned = re.sub(r"^```(?:json)?|```$", "", text.strip(), flags=re.MULTILINE)
    logging.info("Cleaned response text.")
    return cleaned.strip()

def process_all_receipts():
    """Iterate through all receipts in the sheet and write results to PROCESSED column."""
    # Get all headers
    logging.info("Starting batch receipt processing and upload...")

    headers = sheet.row_values(1)

    if "IMAGE FILENAME" not in headers or "EXTRACTED TEXT" not in headers or "PROCESSED" not in headers:
        logging.error("❌ Missing required headers: IMAGE FILENAME / EXTRACTED TEXT / PROCESSED")
        return

    filename_col = headers.index("IMAGE FILENAME") + 1
    extracted_col = headers.index("EXTRACTED TEXT") + 1
    processed_col = headers.index("PROCESSED") + 1

    all_rows = sheet.get_all_values()

    for i, row in enumerate(all_rows[1:], start=2):  # skip header row
        filename = row[filename_col - 1].strip() if len(row) >= filename_col else f"Row{i}"
        receipt_text = row[extracted_col - 1].strip() if len(row) >= extracted_col else ""
        processed_text = row[processed_col - 1].strip() if len(row) >= processed_col else ""

        if not receipt_text:
            logging.info(f"Skipping empty row {i}: {filename}")
            continue
        if processed_text:
            logging.info(f"Already processed row {i}: {filename}")
            continue

        logging.info(f"🧾 Processing row {i}: {filename}")
        result = send_to_ollama(receipt_text)

        if result:
            try:
                cleaned = clean_json_text(result)
                data = json.loads(cleaned)
                logging.info(f"✅ Valid JSON for {filename}")
                logging.info(f"Data: {data}")
                # --- Write clean JSON back to sheet ---
                sheet.update_cell(i, processed_col, json.dumps(data, ensure_ascii=False))
                logging.info(f"📤 Written to PROCESSED (row {i})")

            except json.JSONDecodeError:
                logging.warning(f"⚠️ Non-JSON for {filename}: {result[:100]}...")
        else:
            logging.error(f"❌ No response for {filename}")

        time.sleep(1.5)  # Optional delay between rows
    logging.info("✅ All receipts processed and uploaded to Google Sheet.")


# ---- Main ----
if __name__ == "__main__":
    get_gsheet()
    process_all_receipts()
