package com.example.ai_gen_amz_content;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
public class PdfController {

    @PostMapping("/extract-pdf")
    public ResponseEntity<Map<String, Object>> extractPdf(@RequestParam("file") MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            // Kiểm tra số trang
            int numberOfPages = document.getNumberOfPages();
            log.info("Number of pages: {}", numberOfPages);

            if (numberOfPages == 0) {
                throw new IOException("PDF has no pages");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(numberOfPages);

            String text = stripper.getText(document);

            if (text.trim().isEmpty()) {
                throw new IOException("Unable to extract any text from PDF");
            }

            log.info("Extracted text length: {}", text.length());
            log.info("First 500 chars: {}", text.substring(0, Math.min(500, text.length())));

            // Split text into lines for easier parsing
            String[] lines = text.split("\n");

            // Parse multi-order by splitting on "Ship To:" as block starters
            List<Map<String, Object>> orders = new ArrayList<>();
            List<String> currentBlock = new ArrayList<>();
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("Ship To:")) {
                    if (!currentBlock.isEmpty()) {
                        processOrderBlock(currentBlock, orders);
                    }
                    currentBlock = new ArrayList<>();
                }
                currentBlock.add(line);
            }
            if (!currentBlock.isEmpty()) {
                processOrderBlock(currentBlock, orders);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("orders", orders);
            return ResponseEntity.ok(response);
        }
    }

    private void processOrderBlock(List<String> blockLines, List<Map<String, Object>> orders) {
        Map<String, Object> order = new HashMap<>();
        String orderId = "";
        String orderDate = "";
        String buyerName = "";
        String sellerName = "";
        String shippingService = "";
        String grandTotal = "";

        Map<String, String> shipping = new HashMap<>();
        shipping.put("country", "United States"); // Hardcoded as per examples

        List<Map<String, String>> products = new ArrayList<>();

        // Find shipping address from "Ship To:"
        int shipToIndex = -1;
        for (int i = 0; i < blockLines.size(); i++) {
            if (blockLines.get(i).trim().startsWith("Ship To:")) {
                shipToIndex = i;
                break;
            }
        }
        if (shipToIndex != -1 && shipToIndex + 3 < blockLines.size()) {
            shipping.put("recipientName", blockLines.get(shipToIndex + 1).trim());
            shipping.put("streetAddress", blockLines.get(shipToIndex + 2).trim());
            String cityStateZip = blockLines.get(shipToIndex + 3).trim();
            // Handle split zip if next line is digits (e.g., '2005')
            int offset = 4;
            while (shipToIndex + offset < blockLines.size() && blockLines.get(shipToIndex + offset).trim().matches("\\d+.*")) {
                cityStateZip += "-" + blockLines.get(shipToIndex + offset).trim();
                offset++;
            }
            // Parse city, state, zip with regex
            Pattern addressRegex = Pattern.compile("(.*?),\\s*([^\\d]+)\\s*(\\d{5}(?:-\\d{4})?)");
            Matcher addressMatcher = addressRegex.matcher(cityStateZip);
            if (addressMatcher.find()) {
                shipping.put("city", addressMatcher.group(1).trim());
                shipping.put("state", addressMatcher.group(2).trim());
                shipping.put("zipCode", addressMatcher.group(3).trim());
            }
        }

        // Parse order info and products line by line
        boolean inTable = false;
        Map<String, String> product = null;
        StringBuilder titleBuilder = new StringBuilder();
        StringBuilder customBuilder = new StringBuilder();
        boolean inCustom = false;
        for (String line : blockLines) {
            String trimmed = line.trim();

            if (trimmed.contains("Returning your item:")) {
                break; // Stop processing at returning section to avoid junk
            }

            if (trimmed.startsWith("https://sellercentral") || trimmed.matches("\\d+:\\d+ \\d+/\\d+/\\d+ Amazon")) {
                continue; // Skip footer lines
            }

            if (trimmed.contains("Order ID:")) {
                orderId = extractWithRegex(trimmed, "Order ID: (\\d+-\\d+-\\d+)");
            } else if (trimmed.contains("Order Date:")) {
                orderDate = extractWithRegex(trimmed, "Order Date: (.*)");
            } else if (trimmed.contains("Shipping Service:")) {
                shippingService = extractWithRegex(trimmed, "Shipping Service: (.*)");
            } else if (trimmed.contains("Buyer Name:")) {
                buyerName = extractWithRegex(trimmed, "Buyer Name: (.*)");
            } else if (trimmed.contains("Seller Name:")) {
                sellerName = extractWithRegex(trimmed, "Seller Name: (.*)");
            } else if (trimmed.contains("Grand total:")) {
                grandTotal = extractWithRegex(trimmed, "Grand total: \\$([\\d.]+)");
            } else if (trimmed.contains("Quantity") && trimmed.contains("Product Details")) {
                inTable = true;
                continue;
            } else if (inTable) {
                if (trimmed.contains("Grand total")) {
                    grandTotal = extractWithRegex(trimmed, "Grand total: \\$([\\d.]+)");
                    inTable = false;
                    inCustom = false;
                    continue;
                }
                if (trimmed.matches("\\d+.*")) {
                    // New product starts
                    if (product != null) {
                        product.put("productTitle", titleBuilder.toString().trim());
                        product.put("customizations", customBuilder.toString().trim());
                        products.add(product);
                    }
                    product = new HashMap<>();
                    titleBuilder = new StringBuilder();
                    customBuilder = new StringBuilder();
                    inCustom = false;
                    Pattern quantityPattern = Pattern.compile("(\\d+)\\s+(.*)\\s+\\$([\\d.]+)");
                    Matcher m = quantityPattern.matcher(trimmed);
                    if (m.find()) {
                        product.put("quantity", m.group(1));
                        titleBuilder.append(m.group(2));
                        product.put("unitPrice", "$" + m.group(3));
                    } else {
                        titleBuilder.append(" " + trimmed);
                    }
                } else if (trimmed.startsWith("Item subtotal")) {
                    product.put("itemSubtotal", extractWithRegex(trimmed, "Item subtotal \\$([\\d.]+)"));
                } else if (trimmed.startsWith("SKU:")) {
                    product.put("sku", extractWithRegex(trimmed, "SKU: (\\S+)"));
                    if (trimmed.contains("Shipping total")) {
                        product.put("shippingTotal", extractWithRegex(trimmed, "Shipping total \\$([\\d.]+)"));
                    }
                } else if (trimmed.startsWith("ASIN:")) {
                    product.put("asin", extractWithRegex(trimmed, "ASIN: (\\S+)"));
                    if (trimmed.contains("Tax")) {
                        product.put("tax", extractWithRegex(trimmed, "Tax \\$([\\d.]+)"));
                    }
                } else if (trimmed.startsWith("Condition:")) {
                    product.put("condition", extractWithRegex(trimmed, "Condition: (\\S+)"));
                    if (trimmed.contains("Item total")) {
                        product.put("itemTotal", extractWithRegex(trimmed, "Item total \\$([\\d.]+)"));
                    }
                } else if (trimmed.startsWith("Item total")) {
                    product.put("itemTotal", extractWithRegex(trimmed, "Item total \\$([\\d.]+)"));
                } else if (trimmed.startsWith("Order Item ID:")) {
                    product.put("orderItemId", extractWithRegex(trimmed, "Order Item ID: (\\S+)"));
                } else if (trimmed.startsWith("Customizations")) {
                    inCustom = true;
                    customBuilder.append(trimmed.replace("Customizations:", "").trim() + " ");
                } else if (inCustom) {
                    if (trimmed.startsWith("https://sellercentral") || trimmed.contains("Grand total") || trimmed.matches("\\d+:\\d+ \\d+/\\d+/\\d+ Amazon") || trimmed.isEmpty()) {
                        inCustom = false;
                        continue;
                    }
                    customBuilder.append(trimmed + " ");
                } else {
                    // Append to title if not a total line
                    if (!trimmed.startsWith("Shipping total") && !trimmed.startsWith("Tax") && !trimmed.startsWith("Item total")) {
                        titleBuilder.append(" " + trimmed);
                    }
                }
            }
        }
        // Add the last product
        if (product != null) {
            product.put("productTitle", titleBuilder.toString().trim());
            product.put("customizations", customBuilder.toString().trim());
            products.add(product);
        }

        order.put("orderId", orderId);
        order.put("orderDate", orderDate);
        order.put("buyerName", buyerName);
        order.put("sellerName", sellerName);
        order.put("shippingService", shippingService);
        order.put("grandTotal", grandTotal);
        order.put("shippingAddress", shipping);
        order.put("products", products);

        if (!orderId.isEmpty() && !products.isEmpty()) { // Only add if valid order with products
            orders.add(order);
        }
    }

    private String extractWithRegex(String text, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }
}