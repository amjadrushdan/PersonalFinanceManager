import os
from io import BytesIO
from googleapiclient.discovery import build
from google.oauth2 import service_account
from google.cloud import vision
from googleapiclient.http import MediaIoBaseDownload

# ----------------------------
# CONFIG: Service Account
# ----------------------------
# Use a relative path to the JSON inside the function folder
SERVICE_ACCOUNT_FILE = os.path.join(os.path.dirname(__file__), "pfm-service-account.json")

# Scopes required
SCOPES = [
    "https://www.googleapis.com/auth/drive.readonly",
    "https://www.googleapis.com/auth/spreadsheets"
]

# Set environment variable for Google APIs
os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = SERVICE_ACCOUNT_FILE

# Create credentials object
credentials = service_account.Credentials.from_service_account_file(
    SERVICE_ACCOUNT_FILE, scopes=SCOPES
)

# ----------------------------
# Environment variables from Cloud Function
# ----------------------------
FOLDER_ID = os.environ.get("FOLDER_ID")
SHEET_ID = os.environ.get("SHEET_ID")

# ----------------------------
# Initialize clients
# ----------------------------
drive_service = build("drive", "v3", credentials=credentials)
sheets_service = build("sheets", "v4", credentials=credentials)
vision_client = vision.ImageAnnotatorClient()

# Keep track of processed files (demo only; use persistent storage in production)
processed_files = set()

# ----------------------------
# Cloud Function entry point
# ----------------------------
def process_drive_folder(request):
    """
    HTTP-triggered Cloud Function to process images in a Drive folder.
    """
    global processed_files

    # List images in Drive folder
    results = drive_service.files().list(
        q=f"'{FOLDER_ID}' in parents and mimeType contains 'image/' and trashed = false",
        fields="files(id, name)"
    ).execute()
    
    files = results.get("files", [])
    if not files:
        return "No images found.", 200

    for file in files:
        file_id = file["id"]
        file_name = file["name"]

        # Skip already processed files
        if file_id in processed_files:
            continue

        # Download image content
        request_file = drive_service.files().get_media(fileId=file_id)
        fh = BytesIO()
        downloader = MediaIoBaseDownload(fh, request_file)
        done = False
        while not done:
            status, done = downloader.next_chunk()
        image_content = fh.getvalue()

        # OCR via Google Vision
        image = vision.Image(content=image_content)
        response = vision_client.text_detection(image=image)
        text = response.text_annotations[0].description if response.text_annotations else ""

        # Append to Google Sheet (Sheet2: ExtractedText)
        sheets_service.spreadsheets().values().append(
            spreadsheetId=SHEET_ID,
            range="ExtractedText!A1",
            valueInputOption="RAW",
            body={"values": [[file_name, text]]}
        ).execute()

        print(f"Processed file: {file_name}, text length: {len(text)}")
        processed_files.add(file_id)

    return f"Processed {len(files)} files.", 200
