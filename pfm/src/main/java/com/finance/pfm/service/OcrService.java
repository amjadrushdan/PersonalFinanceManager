// package com.finance.pfm.service;

// import com.finance.pfm.model.OcrResult;
// import net.sourceforge.tess4j.*;
// import net.sourceforge.tess4j.util.LoadLibs;
// import org.springframework.stereotype.Service;

// import java.io.File;
// import java.util.*;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;
// import net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel;
// import net.sourceforge.tess4j.Word;

// @Service
// public class OcrService {

//     private final ITesseract tesseract;

//     public OcrService() {
//         tesseract = new Tesseract();

//         // Option A: use system-installed tesseract (recommended)
//         // comment/uncomment depending on your setup
//         // tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata"); // linux example

//         // Option B: let tess4j extract packaged tessdata (works for many setups)
//         File tessDataFolder = LoadLibs.extractTessResources("tessdata");
//         tesseract.setDatapath(tessDataFolder.getAbsolutePath());

//         // if receipts are in English and Malay, and you installed both, you can use "eng+msa"
//         tesseract.setLanguage("eng+ms");
//     }

//     public OcrResult extractReceipt(File imageFile) throws TesseractException {
//         String fullText = tesseract.doOCR(imageFile);

//         // words with confidence
//         List<Word> words = Collections.emptyList();
//         try {
//             words = tesseract.getWords(imageFile, TessPageIteratorLevel.RIL_WORD);
//         } catch (TesseractException e) {
//             // fallback; leave words empty
//         }

//         // compute average confidence (word confidences are 0-100)
//         double avgConf = 0.0;
//         if (!words.isEmpty()) {
//             avgConf = words.stream()
//                     .mapToDouble(w -> {
//                         try { return w.getConfidence(); } catch (Exception ex) { return 0.0; }
//                     })
//                     .average().orElse(0.0) / 100.0; // scale to 0..1
//         }

//         Double amount = parseAmount(fullText);
//         String description = parseDescription(fullText, amount);

//         return new OcrResult(amount, description, avgConf, fullText);
//     }

//     // Heuristic: first look for "RM" amounts, else pick the largest number (likely total)
//     private Double parseAmount(String text) {
//         if (text == null) return null;

//         // 1) find RM patterns like "RM12.50" or "RM 12.50"
//         Pattern pRm = Pattern.compile("(?i)rm\\s*([0-9]+(?:\\.[0-9]{1,2})?)");
//         Matcher m = pRm.matcher(text);
//         List<Double> amounts = new ArrayList<>();
//         while (m.find()) {
//             try { amounts.add(Double.parseDouble(m.group(1))); } catch (Exception ignored) {}
//         }
//         if (!amounts.isEmpty()) {
//             // often the total is the largest RM found on the receipt
//             return Collections.max(amounts);
//         }

//         // 2) fallback: find all numbers with decimals and choose the largest
//         Pattern pNum = Pattern.compile("([0-9]{1,6}(?:\\.[0-9]{1,2})?)");
//         Matcher m2 = pNum.matcher(text);
//         while (m2.find()) {
//             try { amounts.add(Double.parseDouble(m2.group(1))); } catch (Exception ignored) {}
//         }
//         if (!amounts.isEmpty()) return Collections.max(amounts);

//         return null; // could not find
//     }

//     // Simple heuristic for description: line with merchant name or first line containing letters
//     private String parseDescription(String text, Double amountFound) {
//         if (text == null) return "receipt";

//         String[] lines = text.split("\\r?\\n");
//         // prefer lines near a "TOTAL" match or top lines
//         for (String line : lines) {
//             String l = line.trim();
//             if (l.length() == 0) continue;
//             // ignore numeric lines
//             if (l.matches("^\\d+$")) continue;
//             // if line contains "total" skip as description
//             if (l.toLowerCase().contains("total") || l.toLowerCase().contains("subtotal")) continue;
//             // take the first line that has letters and not just numbers
//             if (l.matches(".*[a-zA-Z].*")) return l;
//         }
//         return "receipt";
//     }
// }
