package google;

/*
 * BEFORE RUNNING:
 * ---------------
 * 1. If not already done, enable the Google Sheets API
 *    and check the quota for your project at
 *    https://console.developers.google.com/apis/api/sheets
 * 2. Install the Java client library on Maven or Gradle. Check installation
 *    instructions at https://github.com/google/google-api-java-client.
 *    On other build systems, you can add the jar files to your project from
 *    https://developers.google.com/resources/api-libraries/download/sheets/v4/java
 */

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.*;
import parser.Ad;

import java.awt.Color;
import java.io.*;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
public class SheetsExample {

    private static Drive driveService;
    private static Sheets sheetsService;

    private static HttpTransport HTTP_TRANSPORT;
    private static JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static String KEY_FILE = "olx-parser.json";
    private static String APPLICATION_NAME = "OLX Parser";

    private static Logger log = Logger.getLogger("");

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            sheetsService = createSheetsService();
            driveService = createDriveService();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Color convertRgba(Color fgColor) {
        int r, g, b;
        r = fgColor.getRed() * fgColor.getAlpha() + 255 * (255 - fgColor.getAlpha());
        g = fgColor.getGreen() * fgColor.getAlpha() + 255 * (255 - fgColor.getAlpha());
        b = fgColor.getBlue() * fgColor.getAlpha() + 255 * (255 - fgColor.getAlpha());
        return new Color(r / 255, g / 255, b / 255);
    }

    public static String generateSheet(String title, List<Ad> ads, ReportFilter filters) throws Exception {

        int descLength = 0;

        boolean offlineMod = false;
//        if (filters.isDescription()) {
//            for (Ad ad : ads)
//                descLength += ad.getText() == null ? 0 : ad.getText().length();
//
//            offlineMod = descLength > 3_000_000;
//        }

        ColorData colorData = new ColorData(ads);

        try {
            // 1. CREATE NEW SPREADSHEET
            Spreadsheet requestBody = new Spreadsheet();
            SpreadsheetProperties spreadProp = new SpreadsheetProperties();
            spreadProp.setTitle(title);
            spreadProp.setLocale("ru_RU");
            spreadProp.setTimeZone("Europe/Moscow");
            requestBody.setProperties(spreadProp);


            List<Sheet> sheets = new ArrayList<>();

            // -------------------- MAIN SHEET --------------------
            Sheet mainSheet = new Sheet();

            SheetProperties sheetProperties = new SheetProperties();
            sheetProperties.setTitle("Объявления");
            sheetProperties.setSheetId(0);
            GridProperties gridProperties = new GridProperties();
            gridProperties.setFrozenRowCount(1);
            sheetProperties.setGridProperties(gridProperties);
            mainSheet.setProperties(sheetProperties);

            List<GridData> gData = new ArrayList<>();
            GridData gridData = new GridData();
            gData.add(gridData);

            List<RowData> rData = new ArrayList<>();

            // -------------------- SET HEADERS --------------------
            rData.add(getRowHeaders(filters));

            // -------------------- SET VALUES --------------------
            for (Ad ad : ads) {
                RowData rowVal = getRowData(filters, offlineMod, ad, colorData);
                if (rowVal == null) continue;
                rData.add(rowVal);
            }
            // -------------------- SET VALUES ( END ) --------------------

//            gridData.setRowData(rData);
            mainSheet.setData(gData);
            sheets.add(mainSheet);
            // -------------------- MAIN SHEET ( END ) --------------------

            // -------------------- SORTS SHEETS --------------------
//            sheets.add(getSortSheet("Цены (сорт)", "=SORT('Объявления'!A2:U20000,2,FALSE)", filters));
//            sheets.add(getSortSheet("Просм. Всего", "=SORT('Объявления'!A2:U20000;3;FALSE)", filters));
//            sheets.add(getSortSheet("Методы (сорт)", "=SORT('Объявления'!A2:U20000;5;FALSE)", filters));
//            sheets.add(getSortSheet("Просм. За день", "=SORT('Объявления'!A2:U20000;4;FALSE)", filters));
//            if (filters.isDate()) {
//                sheets.add(getSortSheet("Просм. 10 дней", "=SORT('Объявления'!A2:U20000;17;FALSE)", filters));
//                sheets.add(getSortSheet("max. Просм. 10дн.", "=SORT('Объявления'!A2:U20000;18;FALSE)", filters));
//                sheets.add(getSortSheet("Просм. ср. 10 дней", "=SORT('Объявления'!A2:U20000;20;FALSE)", filters));
//            }

            // -------------------- STATISTIC SHEET --------------------
            if (filters.isDate()) {
                sheets.add(getViewSheet(ads));
                sheets.add(getCompetitorSheet(ads, filters));
            }
            sheets.add(getStatisticSheet(ads, filters));

            requestBody.setSheets(sheets);
            Sheets.Spreadsheets.Create request = sheetsService.spreadsheets().create(requestBody);


            Spreadsheet response = null;
            for (int i = 0; i < 2; i++) {
                try {
                    response = request.execute();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    Thread.sleep(5000);
                }
            }

            if (response == null) throw new Exception("Не удалось получить ответ от Google API");
            // 2. PUBLISH SPREADSHEAT VIA DRIVE API
            String fileId = response.getSpreadsheetId();
            setPermission(fileId, offlineMod);

            if (offlineMod) {
                updateDescTableNew(ads, fileId);
//                updateDescTable(ads, fileId);

                InetAddress localHost = InetAddress.getLocalHost();
                title = title.replaceAll("\\s\\|\\s\\d+-.*$", "");
                title = title.replaceAll("\\s\\|\\s", "_");
                title = URLEncoder.encode(title, "UTF-8").replaceAll("\\+", "%20") + ".xslx";

                return String.format("http://%s:8081/api/report/%s?fileID=%s", localHost.getHostAddress(), title, fileId);
            } else {
                if (filters.isDate()) {
                    try {
                        List<Request> requests = new ArrayList<>();
                        requests.add(createCellSizeRequest(0, 4, 150));
                        requests.add(createCellSizeRequest(4, 5, 20));
                        requests.add(createCellSizeRequest(5, 9, 150));

//                        int reqSize = filters.isDescription() ? 2000 : 4000;
                        int reqSize = 500;
                        int check = 1;
                        List<RowData> tmpData = new ArrayList<>(reqSize + 1);
                        Iterator<RowData> rIter = rData.iterator();
                        while (rIter.hasNext()) {
                            if (tmpData.size() > reqSize) {
                                requests.add(new Request().setAppendCells(new AppendCellsRequest().setSheetId(0).setFields("*").setRows(tmpData)));
                                BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(requests);

                                try {
                                    sheetsService.spreadsheets().batchUpdate(response.getSpreadsheetId(), body).execute();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    break;
                                } finally {
                                    System.out.println(check++);
                                }

                                Thread.sleep(250);
                                tmpData.clear();
                                request.clear();
                            } else {
                                tmpData.add(rIter.next());
                                rIter.remove();
                            }
                        }

//                        requests.add(new Request().setAppendCells(new AppendCellsRequest().setSheetId(0).setFields("*").setRows(tmpData)));
//                        BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(requests);
//
//                        sheetsService.spreadsheets().batchUpdate(response.getSpreadsheetId(), body).execute();

                        tmpData.clear();
                        request.clear();
                    } catch (Exception ignore) {
                        ignore.printStackTrace();
                    }
                }

                return response.getSpreadsheetUrl();
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Не удалось сформировать отчёт");
            e.printStackTrace();
            throw new Exception((e.getMessage().contains("Google API") ?
                    "Не удалось получить ответ от Google API" :
                    "Не удалось сформировать отчёт"));
        }
    }

    private static RowData getRowData(ReportFilter filters, boolean offlineMod, Ad ad, ColorData colorData) {


        RowData rowVal = new RowData();
        List<CellData> clValues = new ArrayList<>();

        if (filters.isPosition()) {
            Integer position = 0;
            try {
                position = ad.getPosition();
            } catch (Exception ignore) {
            }

            double coff = (position / 00.7) / 100;
            int alpha = (int) (255 * (coff > 1 ? 0 : (1 - coff)));
            clValues.add(getCellData(position, new Color(183, 225, 205, alpha)));
        }

        String titleName = "";
        try {
            titleName = ad.getTitle();
        } catch (Exception ignore) {
        }
        if (titleName.isEmpty()) return null;
        clValues.add(getCellData(titleName));

        try {
            String price = ad.getPrice();
            try {
                int priceInt = Integer.parseInt(price.replaceAll(" ", ""));
                clValues.add(getCellData(priceInt));
            } catch (Exception ignored) {
                clValues.add(getCellData(price));
            }
        } catch (Exception ignore) {
        }

        try {
            if (ad.getPriceDown())
                clValues.add(getCellData(1));
            else
                clValues.add(getCellData(0));
        } catch (Exception ignore) {
            clValues.add(getCellData(0));
        }

        String views = "";
        try {
            views = ad.getViews();

            double coff = (Integer.parseInt(views) / ((colorData.getMaxViews() - colorData.getMinViews()) / 100.0)) / 100;
            int alpha = (int) (255 * (colorData.isEmpty ? 0 : coff));

            clValues.add(getCellData(Integer.parseInt(views), new Color(255, 214, 102, alpha)));
        } catch (Exception ignore) {
            ignore.printStackTrace();
            clValues.add(getCellData(0));
        }


        String dailyViews = "";
        try {
            dailyViews = ad.getDailyViews();
            double coff = (Integer.parseInt(dailyViews) / ((colorData.getMaxDailyViews() - colorData.getMinDailyViews()) / 100.0)) / 100;
            int alpha = (int) (255 * (colorData.isEmpty ? 0 : coff));
            clValues.add(getCellData(Integer.parseInt(dailyViews), new Color(147, 196, 125, alpha)));
        } catch (Exception ignore) {
            clValues.add(getCellData(0));
        }

        String viewYesterday = "";
        try {
            viewYesterday = ad.getViewYesterday();
            double coff = (Integer.parseInt(viewYesterday) / ((colorData.getMaxViewYesterday() - colorData.getMinViewYesterday()) / 100.0)) / 100;
            int alpha = (int) (255 * (colorData.isEmpty ? 0 : coff));
            clValues.add(getCellData(Integer.parseInt(viewYesterday), new Color(60, 120, 216, alpha)));
        } catch (Exception ignore) {
            clValues.add(getCellData(0));
        }


        if (filters.isDate()) {
            if (ad.hasStats() != null && ad.hasStats()) {

                try {
                    String viewsTenDay = ad.getViewsTenDay();

                    double coff = (Integer.parseInt(viewsTenDay) / ((colorData.getMaxViewsTenDay() - colorData.getMinViewsTenDay()) / 100.0)) / 100;
                    int alpha = (int) (255 * (colorData.isEmpty ? 0 : coff));
                    clValues.add(getCellData(Integer.parseInt(viewsTenDay), new Color(194, 123, 160, alpha)));
                } catch (Exception ignore) {
                    clValues.add(getCellData(0));
                }

                try {
                    Integer maxTenDay = ad.getMaxTenDay();
                    double coff = (maxTenDay / ((colorData.getMaxMaxTenDay() - colorData.getMinMaxTenDay()) / 100.0)) / 100;
                    int alpha = (int) (255 * (colorData.isEmpty ? 0 : coff));
                    clValues.add(getCellData(maxTenDay, new Color(87, 187, 138, alpha)));
                } catch (Exception ignore) {
                    clValues.add(getCellData(0));
                }

                try {
                    String maxTenDate = ad.getMaxTenDate();
                    clValues.add(getCellData(maxTenDate));
                } catch (Exception ignore) {
                    clValues.add(getCellData(0));
                }

                try {
                    String viewsAverageTenDay = ad.getViewsAverageTenDay();
                    double coff = (Integer.parseInt(viewsAverageTenDay) / ((colorData.getMaxViewsAverageTenDay() - colorData.getMinViewsAverageTenDay()) / 100.0)) / 100;
                    int alpha = (int) (255 * (colorData.isEmpty ? 0 : coff));
                    clValues.add(getCellData(Integer.parseInt(viewsAverageTenDay), new Color(234, 153, 153, alpha)));
                } catch (Exception ignore) {
                    clValues.add(getCellData(0));
                }

            } else {

                try {
                    clValues.add(getCellData(Integer.parseInt(ad.getViews())));
                } catch (Exception ignore) {
                    clValues.add(getCellData(0));
                }

                try {
                    clValues.add(getCellData(Integer.parseInt(ad.getViews())));
                } catch (Exception e) {
                    clValues.add(getCellData(0));
                }

                clValues.add(getCellData(new SimpleDateFormat("yyyy.MM.dd").format(new Date())));

                try {
                    clValues.add(getCellData(Integer.parseInt(ad.getViews())));
                } catch (Exception ignore) {
                    clValues.add(getCellData(0));
                }
            }
        }


        String services = "";
        try {
            if (ad.getPremium()) {
                services = services + "1 ";
            }
            if (ad.getVip()) {
                services = services + "2 ";
            }
            if (ad.getUrgent()) {
                services = services + "3 ";
            }
            if (ad.getUpped()) {
                services = services + "4 ";
            }
            if (ad.getXL()) {
                services = services + "5 ";
            }
        } catch (Exception ignore) {
        }
        clValues.add(getCellData(services));

        String data = "";
        try {
            data = ad.getData();
        } catch (Exception ignore) {
        }
        clValues.add(getCellData(data));


        if (filters.isDate()) {
            if (ad.hasStats() != null && ad.hasStats()) {

                String dateApplication = "";
                try {
                    dateApplication = ad.getDateApplication();
                } catch (Exception ignore) {
                }
                clValues.add(getCellData(dateApplication));

            } else {
                clValues.add(getCellData(new SimpleDateFormat("yyyy.MM.dd").format(new Date())));

            }
        }

        if (filters.isPhoto()) {
            String numberPictures = "";
            try {
                numberPictures = ad.getNumberPictures();
            } catch (Exception ignore) {
            }
            clValues.add(getCellData(numberPictures));
        }

        if (filters.isDescription()) {
            String text = "";
            try {
                if (offlineMod) {
                    text = ad.getId();
                } else {
                    text = ad.getText();
                    text = text.trim();
                }
            } catch (Exception ignore) {
            }
            clValues.add(getCellData(text.replace("\u00A0", " ").trim()));
        }

        if (filters.isDescriptionLength()) {
            String quantityText = "";
            try {
                quantityText = ad.getQuantityText();
            } catch (Exception ignore) {
            }
            clValues.add(getCellData(quantityText));
        }

        int delivery = 0;
        try {
            if (ad.getDelivery()) {
                delivery = 1;
            }
        } catch (Exception ignore) {
        }
        clValues.add(getCellData(delivery));

        if (filters.isSellerName()) {
            String seller = "";
            try {
                seller = ad.getSeller();
            } catch (Exception ignore) {
            }
            clValues.add(getCellData(seller));
        }

        String sellerId = "";
        try {
            sellerId = ad.getSellerId().substring(ad.getSellerId().indexOf("=") + 1);
            int alpha = 255;
            if (ad.getShop()) {
                clValues.add(getCellData(sellerId, new Color(252, 232, 178, alpha)));
            } else {
                clValues.add(getCellData(sellerId, new Color(255, 255, 255, alpha)));
            }
        } catch (Exception ignore) {
            clValues.add(getCellData(0));
        }


        if (filters.isPhone()) {
            String phone = "";
            try {
                phone = ad.getPhone();
            } catch (Exception ignore) {
            }
            clValues.add(getCellData(phone));
        }

        String activeAd = "0";
        try {
            activeAd = ad.getActiveAd();
        } catch (Exception ignore) {
        }
        clValues.add(getCellData(activeAd));

        String address = "";
        try {
            address = ad.getAddress();
        } catch (Exception ignore) {
        }
        clValues.add(getCellData(address));


        String url = "";
        try {
            url = ad.getUrl();
        } catch (Exception ignore) {
        }
        clValues.add(getCellData(url));


        rowVal.setValues(clValues);
        return rowVal;
    }

    private static Request createCellSizeRequest(Integer startIndex, Integer endIndex, Integer size) {
        return new Request().setUpdateDimensionProperties(
                new UpdateDimensionPropertiesRequest().setRange
                        (
                                new DimensionRange()
                                        .setSheetId(1)
                                        .setDimension("COLUMNS")
                                        .setStartIndex(startIndex).setEndIndex(endIndex)
                        )
                        .setProperties(new DimensionProperties().setPixelSize(size)).setFields("pixelSize")
        );
    }

    @SuppressWarnings("Duplicates")
    private static void updateDescTableNew(List<Ad> ads, String fileId) throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook(new FileInputStream("reports/" + fileId + ".xlsx"));
        while (true) {
            XSSFSheet sheet = wb.getSheetAt(1);
            if (sheet.getSheetName().equals("Статистика"))
                break;

            wb.removeSheetAt(1);
        }

        int descOffset = -1;
        XSSFRow colNames = wb.getSheetAt(0).getRow(0);
        for (int i = 0; i < colNames.getLastCellNum(); i++) {
            String colName = colNames.getCell(i).getStringCellValue();
            if (colName.equals("Текст")) {
                descOffset = i;
                break;
            }
        }

        XSSFSheet sheet = wb.getSheetAt(0);
        for (Integer i = 1; i < sheet.getLastRowNum(); i++) {
            XSSFRow row = sheet.getRow(i);

            for (int j = 0; j < row.getLastCellNum(); j++) {
                XSSFCell cell = row.getCell(j);

                if (descOffset == j) {
                    String id = cell.getStringCellValue();

                    Optional<Ad> findAd = ads.stream().filter(ad -> ad.getId().equals(id)).findFirst();
                    String desc = findAd.isPresent() ? findAd.get().getText() : "";

                    row.getCell(descOffset).setCellValue(desc.replace("\u00A0", " ").trim());
                }
            }
        }

        FileOutputStream outputStream = new FileOutputStream("reports/" + fileId + ".xlsx");
        wb.write(outputStream);
        wb.close();
    }

    public static void updateDescTable(List<Ad> ads, String fileId) throws IOException {

        XSSFWorkbook wb = new XSSFWorkbook(new FileInputStream("reports/" + fileId + ".xlsx"));

        int descOffset = -1;
        XSSFRow colNames = wb.getSheetAt(0).getRow(0);
        for (int i = 0; i < colNames.getLastCellNum(); i++) {
            XSSFCell cell = colNames.getCell(i);
            String colName = cell.getStringCellValue();
            if (colName.equals("Текст")) {
                descOffset = i;
                break;
            }
        }

        List<XSSFCell[]> rows = new ArrayList<>(wb.getSheetAt(0).getLastRowNum());
        for (int sheetIndex = 0; sheetIndex < wb.getNumberOfSheets() - 1; sheetIndex++) {

            XSSFSheet sheet = wb.getSheetAt(sheetIndex);
            if (sheetIndex != 0) {
                XSSFCell cell = sheet.getRow(1).getCell(0);
                int sortedCol = Integer.parseInt(cell.getCellFormula().split(",")[1]) - 1;
                sheet.removeArrayFormula(cell);

                System.out.println(sheetIndex);
                rows.sort((o1, o2) -> {
                    try {
                        XSSFCell fCell = o1[sortedCol];
                        XSSFCell sCell = o2[sortedCol];

                        if (fCell.getCellTypeEnum() != sCell.getCellTypeEnum())
                            return fCell.getCellTypeEnum() == CellType.STRING ? -1 : 1;

                        switch (fCell.getCellTypeEnum()) {
                            case STRING:
                                return (fCell.getStringCellValue().compareTo(sCell.getStringCellValue()) * -1);
                            case NUMERIC:
                                return Double.compare(sCell.getNumericCellValue(), fCell.getNumericCellValue());
                            default:
                                return 0;
                        }
                    } catch (Exception e) {
                        return 0;
                    }
                });
            }

            for (Integer i = 1; i < wb.getSheetAt(0).getLastRowNum(); i++) {
                XSSFRow row = sheet.getRow(i);

                if (sheetIndex == 0) {
                    XSSFCell[] data = new XSSFCell[colNames.getLastCellNum()];
                    for (int j = 0; j < row.getLastCellNum(); j++) {
                        XSSFCell cell = row.getCell(j);

                        if (descOffset == j) {
                            String id = cell.getStringCellValue();

                            Optional<Ad> findAd = ads.stream().filter(ad -> ad.getId().equals(id)).findFirst();
                            String desc = findAd.isPresent() ? findAd.get().getText() : "";

                            row.getCell(descOffset).setCellValue(desc.replace("\u00A0", " ").trim());
                        }
                        data[j] = row.getCell(j);
                    }
                    rows.add(data);
                } else {
                    for (int j = 0; j < colNames.getLastCellNum(); j++) {
                        XSSFCell cacheCell = rows.get(i - 1)[j];
                        XSSFCell cell = row.createCell(j);

                        if (cacheCell == null)
                            continue;

//                        cell.setCellStyle(cacheCell.getCellStyle());
                        switch (cacheCell.getCellTypeEnum()) {
                            case STRING:
                                cell.setCellValue(cacheCell.getStringCellValue());
                                break;
                            case NUMERIC:
                                cell.setCellValue(cacheCell.getNumericCellValue());
                                break;
                        }
                    }
                }
            }
        }

        FileOutputStream outputStream = new FileOutputStream("reports/" + fileId + ".xlsx");
//        FileOutputStream outputStream = new FileOutputStream("reports/out.xlsx");
        wb.write(outputStream);
        wb.close();
    }

    private static Sheet getStatisticSheet(List<Ad> ads , ReportFilter filters) {
        Sheet sheet = new Sheet();

        SheetProperties sheetProp = new SheetProperties();
        sheetProp.setTitle("Статистика");
        GridProperties gridProp = new GridProperties();
        sheetProp.setGridProperties(gridProp);
        sheet.setProperties(sheetProp);

        List<GridData> gData = new ArrayList<>();
        GridData gridData = new GridData();
        gData.add(gridData);

        List<RowData> rData = new ArrayList<>();

        // -------------------- SET VALUES --------------------
        rData.add(new RowData().setValues(Arrays.asList(
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Всего:")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Объявлений:")),
                new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЗ('Объявления'!B2:B19998)"))
        )));
        rData.add(new RowData());


        if (filters.isDate()) {
            rData.add(new RowData().setValues(Arrays.asList(
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Методы продвижения:")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("5 - XL")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!L2:L19998;\"*5*\")"))
            )));

            rData.add(new RowData().setValues(Arrays.asList(
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("4 - Поднятие")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!L2:L19998;\"*4*\")"))
            )));

            rData.add(new RowData().setValues(Arrays.asList(
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("3 - Выделение")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!L2:L19998;\"*3*\")"))
            )));

            rData.add(new RowData().setValues(Arrays.asList(
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("2 - VIP")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!L2:L19998;\"*2*\")"))
            )));

            rData.add(new RowData().setValues(Arrays.asList(
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("1 - Премиум")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!L2:L19998;\"*1*\")"))
            )));
            rData.add(new RowData());
        } else {
            rData.add(new RowData().setValues(Arrays.asList(
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Методы продвижения:")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("5 - XL")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!H2:H19998;\"*5*\")"))
            )));

            rData.add(new RowData().setValues(Arrays.asList(
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("4 - Поднятие")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!H2:H19998;\"*4*\")"))
            )));

            rData.add(new RowData().setValues(Arrays.asList(
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("3 - Выделение")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!H2:H19998;\"*3*\")"))
            )));

            rData.add(new RowData().setValues(Arrays.asList(
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("2 - VIP")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!H2:H19998;\"*2*\")"))
            )));

            rData.add(new RowData().setValues(Arrays.asList(
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("1 - Премиум")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!H2:H19998;\"*1*\")"))
            )));
            rData.add(new RowData());
        }
        rData.add(new RowData().setValues(Arrays.asList(
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Цена:")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Минимальная:")),
                new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=МИН('Объявления'!C2:C19998)"))
        )));

        rData.add(new RowData().setValues(Arrays.asList(
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Средняя:")),
                new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СРЗНАЧ('Объявления'!C2:C19998)"))
        )));

        rData.add(new RowData().setValues(Arrays.asList(
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Максимальная:")),
                new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=МАКС('Объявления'!C2:C19998)"))
        )));
        rData.add(new RowData());

        rData.add(new RowData().setValues(Arrays.asList(
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Просмотры:")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Минимальная:")),
                new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=МИН('Объявления'!E2:E19998)"))
        )));

        rData.add(new RowData().setValues(Arrays.asList(
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Средняя:")),
                new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СРЗНАЧ('Объявления'!E2:E19998)"))
        )));

        rData.add(new RowData().setValues(Arrays.asList(
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Максимальная:")),
                new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=МАКС('Объявления'!E2:E19998)"))
        )));
        rData.add(new RowData());

        Boolean coin = true;
        for (String address : new HashSet<>(ads.stream().map(ad -> {
            String address = "";
            if (ad.getAddress().contains("м.")) {
                address = ad.getAddress().substring(ad.getAddress().indexOf("м."));
                address = address.substring(0, address.contains(",") ? address.indexOf(",") : address.length() - 1);
            }
            return address;
        }).collect(Collectors.toList()))) {
            if (address.isEmpty()) continue;
            if (coin) {
                if (filters.isDate()) {
                    rData.add(new RowData().setValues(Arrays.asList(
                            new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Районы:")),
                            new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(address)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!W2:W19998;\"*" + address + "*\")"))
                    )));
                } else {
                    rData.add(new RowData().setValues(Arrays.asList(
                            new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Районы:")),
                            new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(address)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!R2:R19998;\"*" + address + "*\")"))
                    )));
                }
                coin = false;
            } else {
                if (filters.isDate()) {
                    rData.add(new RowData().setValues(Arrays.asList(
                            new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                            new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(address)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!W2:W19998;\"*" + address + "*\")"))
                    )));
                } else {
                    rData.add(new RowData().setValues(Arrays.asList(
                            new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                            new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(address)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!R2:R19998;\"*" + address + "*\")"))
                    )));
                }
            }
        }
        // -------------------- SET VALUES ( END ) --------------------

        gridData.setRowData(rData);
        sheet.setData(gData);
        return sheet;
    }

    private static GridRange generateRange(Integer sheetId, Integer startRowIndex, Integer endRowIndex, Integer startColumnIndex, Integer endColumnIndex) {
        GridRange gRange = new GridRange();
        gRange.setSheetId(sheetId);
        gRange.setStartRowIndex(startRowIndex);
        gRange.setEndRowIndex(endRowIndex);
        gRange.setStartColumnIndex(startColumnIndex);
        gRange.setEndColumnIndex(endColumnIndex);

        return gRange;
    }

    private static Sheet getCompetitorSheet(List<Ad> ads, ReportFilter filters) throws ParseException {
        Sheet sheet = new Sheet();
        SheetProperties sheetProp = new SheetProperties();
        sheetProp.setTitle("Анализ конкурентов");
        sheet.setProperties(sheetProp);

        List<GridData> gData = new ArrayList<>();
        GridData gridData = new GridData();
        gData.add(gridData);

        List<RowData> rData = new ArrayList<>();

        // -------------------- COMPETITOR SHEET --------------------
        List<CompetitorAnalysis> competitors = new ArrayList<>();
        List<Ad> sortedAds = new ArrayList<>();

        List<String> phones = ads.stream().filter(distinctByKey(Ad::getPhone)).map(Ad::getPhone).collect(Collectors.toList());
        for (String phone : phones) {
            List<Ad> allAdsByPhone = ads.stream().filter(ad -> ad.getPhone().equals(phone)).collect(Collectors.toList());
            if (allAdsByPhone.size() > 1) {
                competitors.add(new CompetitorAnalysis(allAdsByPhone));
                sortedAds.addAll(allAdsByPhone);
            }
        }
        // -------------------- SET VALUES --------------------


        // Сводная таблица
        Color headerBgColor = new Color(201, 218, 248);

        CellData header = getCellData("Кол-во объяв.", headerBgColor, true);
        CellData header1 = getCellData("Позиция", headerBgColor, true);
        CellData header2 = getCellData("Кол-во продвигаемых объяв", headerBgColor, true);
        CellData header3 = getCellData("Применяемые Методы", headerBgColor, true);
        CellData header4 = getCellData("Время поднятия", headerBgColor, true);
        CellData header5 = getCellData("Всего Активных объяв.", headerBgColor, true);
        CellData header6 = getCellData("Просм. Всего", headerBgColor, true);
        CellData header7 = getCellData("Просм. Сегодня", headerBgColor, true);
        CellData header8 = getCellData("Просм. Вчера", headerBgColor, true);
        CellData header9 = getCellData("Просм. 10 дней", headerBgColor, true);
        CellData header10 = getCellData("max. Просм. 10дн.", headerBgColor, true);
        CellData header11 = getCellData("Фото (шт)", headerBgColor, true);
        CellData header12 = getCellData("Кол-во знаков", headerBgColor, true);
        CellData header13 = getCellData("Доставка", headerBgColor, true);

        rData.add(new RowData().setValues(Arrays.asList(

                getCellData("№", headerBgColor, true),
                getCellData("Имя продавца", headerBgColor, true),
                getCellData("Телефон", headerBgColor, true),

                header.setNote("Количество объявлений этого продавца в данной выдаче на Авито"),
                header1.setNote("Среднее значение позиций в выдаче, объявлений данного продавца."),
                header2.setNote("Количество объявлений, к которым продавец применяет платные услуги , в данной выдаче."),
                header3.setNote("Все применяемые методы (платные услуги) продавцом, в данной выдаче."),
                header4.setNote("Дата и время поднятия одного из объявлений, собравшее максимальное кол-во просмотров за сегодняшний день с 00:00 часов до момента парсинга."),
                header5.setNote("Количество Активных объявлений у данного продавца, размещенных на Авито в той или иной категории."),
                header6.setNote("Общее количество просмотров на всех объявлениях данного продавца, в данной выдаче."),
                header7.setNote("Общее количество просмотров на всех объявлениях данного продавца за сегодняшний день, с 00:00 часов до момента парсинга."),
                header8.setNote("Общее количество просмотров на всех объявлениях данного продавца за весь вчерашний день, с 00:00 до 24:00 часов"),
                header9.setNote("Общее количество просмотров на всех объявлениях данного продавца за предыдущие 10 дней, включая сегодняшний"),
                header10.setNote("Максимальное количество просмотров на одном из объявлений, за один из предыдущих 10 дней"),
                header11.setNote("Среднее количество фотографий размещеных в объявлениях данного продавца"),
                header12.setNote("Среднее количество знаков (символов) в тексте объявлений данного продавца"),
                header13.setNote("Количество объявлений с указанием доставки")
        )));
        boolean color = false;
        for (int i = 0; i < competitors.size(); i++) {
            CompetitorAnalysis competitor = competitors.get(i);

            int adColor;
            if (color) {
                adColor = 0;
                color = false;
            } else {
                adColor = 255;
                color = true;
            }

            rData.add(new RowData().setValues(Arrays.asList(
                    getCellData(i, new Color(232, 240, 254, adColor)),
                    getCellData(competitor.getSellerTitle(), new Color(232, 240, 254, adColor)),
                    getCellData(competitor.getPhone(), new Color(232, 240, 254, adColor)),
                    getCellData(competitor.getAdCount(), new Color(232, 240, 254, adColor)),
                    getCellData(competitor.getPosition(), new Color(232, 240, 254, adColor)),
                    getCellData(competitor.getPromAd(), new Color(232, 240, 254, adColor)),
                    getCellData(competitor.getPromMethods(), new Color(232, 240, 254, adColor)),
                    getCellData(competitor.getMaxViewDate(), new Color(232, 240, 254, adColor)),
                    getCellData(competitor.getTotalActiveAd(), new Color(232, 240, 254, adColor)),
                    getCellData(competitor.getTotalView(), new Color(232, 240, 254, adColor)),
                    getCellData(competitor.getTodayView(), new Color(232, 240, 254, adColor)),
                    getCellData(competitor.getYesterdayView(), new Color(232, 240, 254, adColor)),
                    getCellData(competitor.getTotalTenDaysView(), new Color(232, 240, 254, adColor)),
                    getCellData(competitor.getMaxTenDaysView(), new Color(232, 240, 254, adColor)),
                    getCellData(competitor.getPhoto(), new Color(232, 240, 254, adColor)),
                    getCellData(competitor.getTextCount(), new Color(232, 240, 254, adColor)),
                    getCellData(competitor.getDelivery(), new Color(232, 240, 254, adColor))
            )));


        }

        // Костыль на пустую строку
        /*rData.add(new RowData().setValues(Collections.singletonList(getCellData(""))));
        rData.add(new RowData().setValues(Collections.singletonList(getCellData(""))));

        filters.setDescription(false);
        rData.add(getRowHeaders(filters));
        // Общая таблица
        boolean colorFlag = false;
        String phone = "";
        for (Ad ad : sortedAds) {
            if (!ad.getPhone().equals(phone)) {
                colorFlag = !colorFlag;
                phone = ad.getPhone();
            }

            com.google.api.services.sheets.v4.model.Color adColor;
            if (colorFlag) {
                adColor = new com.google.api.services.sheets.v4.model.Color()
                        .setRed((float) 0.9098)
                        .setGreen((float) 0.9647)
                        .setBlue((float) 0.9372);
            } else {
                adColor = new com.google.api.services.sheets.v4.model.Color()
                        .setRed((float) 1)
                        .setGreen((float) 1)
                        .setBlue((float) 1);
            }

            RowData rowVal = getRowData(filters, true, ad, new ColorData());
            if (rowVal == null) continue;

            List<CellData> values = rowVal.getValues();
            for (int i = 0; i < values.size(); i++) {
                values.get(i).setUserEnteredFormat(new CellFormat().setBackgroundColor(adColor));
            }

            rData.add(rowVal);
        }*/

        // -------------------- SET VALUES ( END ) --------------------

        gridData.setRowData(rData);
        sheet.setData(gData);
        return sheet;
    }

    private static Sheet getViewSheet(List<Ad> ads) throws ParseException {
        Sheet sheet = new Sheet();
        ViewAnalysis viewAnalysis = new ViewAnalysis(ads);
        SheetProperties sheetProp = new SheetProperties();
        sheetProp.setSheetId(1);
        sheetProp.setTitle("Анализ просмотров");
        sheet.setProperties(sheetProp);

        List<GridData> gData = new ArrayList<>();
        GridData gridData = new GridData();
        gData.add(gridData);

        List<RowData> rData = new ArrayList<>();

        // -------------------- SET VALUES --------------------
        rData.add(new RowData().setValues(Arrays.asList(
                getCellData("Кол-во объявлений созданных за 10 дней", new Color(207, 226, 243, 255), true),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                getCellData("Кол-во объявлений поднятых за 10 дней", new Color(207, 226, 243, 255), true),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                getCellData("Всего просмотров за 10 дн. на всех объявлениях", new Color(207, 226, 243, 255), true),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                getCellData("Всего просмотров за 10 дн. на всех объявлениях", new Color(207, 226, 243, 255), true)
        )));


        for (int i = 0; i < 10; i++) {
            {
                rData.add(new RowData().setValues(Arrays.asList(
                        getCellData(viewAnalysis.tenDays.get(i), new Color(232, 246, 239, 255)),
                        new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.numOfNewAd[i])),
                        getCellData(viewAnalysis.tenDays.get(i), new Color(232, 246, 239, 255)),
                        new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.numOfUpAd[i])),
                        new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                        getCellData(viewAnalysis.tenDays.get(i), new Color(232, 246, 239, 255)),
                        new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.totalViewOfAd[i])),
                        getCellData(viewAnalysis.tenDays.get(i), new Color(232, 246, 239, 255)),
                        new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.avgViewOfAd[i]))
                )));
            }
        }
        rData.add(new RowData().setValues(Arrays.asList(
                getCellData("ВСЕГО:", new Color(239, 239, 239, 255), true),
                getCellData("=СУММ(B2:B11)", new Color(239, 239, 239, 255)),
                getCellData("ВСЕГО:", new Color(239, 239, 239, 255), true),
                getCellData("=СУММ(D2:D11)", new Color(239, 239, 239, 255)),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                getCellData("ВСЕГО:", new Color(239, 239, 239, 255), true),
                getCellData("=СУММ(G2:G11)", new Color(239, 239, 239, 255)),
                getCellData("ВСЕГО:", new Color(239, 239, 239, 255), true),
                getCellData("=СУММ(I2:I11)", new Color(239, 239, 239, 255))
        )));
        rData.add(new RowData());

        // За сегодня ( По часам )
        rData.add(new RowData().setValues(Arrays.asList(
                getCellData("Сегодня", new Color(0, 0, 0, 0), true),
                getCellData(viewAnalysis.tenDays.get(0), new Color(0, 0, 0, 0)),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                getCellData("Сегодня", new Color(0, 0, 0, 0), true),
                getCellData(viewAnalysis.tenDays.get(0), new Color(0, 0, 0, 0))
        )));
        rData.add(new RowData().setValues(Arrays.asList(
                getCellData("Всего просмотров за сегодня на Новых объ-ях", new Color(239, 239, 239, 255), true),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                getCellData("Всего просмотров за сегодня на Поднятых объ-ях", new Color(239, 239, 239, 255), true)
        )));
        rData.add(new RowData().setValues(Arrays.asList(
                getCellData("Часовые интервалы", new Color(207, 226, 243, 255), true),
                getCellData("Кол-во объявлений", new Color(207, 226, 243, 255), true),
                getCellData("Кол-во просмотров", new Color(207, 226, 243, 255), true),
                getCellData("Сред. просмотров", new Color(207, 226, 243, 255), true),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                getCellData("Часовые интервалы", new Color(207, 226, 243, 255), true),
                getCellData("Кол-во объявлений", new Color(207, 226, 243, 255), true),
                getCellData("Кол-во просмотров", new Color(207, 226, 243, 255), true),
                getCellData("Сред. просмотров", new Color(207, 226, 243, 255), true)
        )));

        for (int i = 0; i < 24; i++) {
            {
                if (viewAnalysis.tdayTotalViewOfNewAd[i] != 0 && viewAnalysis.tdayTotalViewOfUpAd[i] != 0) {
                    rData.add(new RowData().setValues(Arrays.asList(
                            getCellData(i, new Color(0, 0, 0, 0), true),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.tdayNumOfNewAd[i])),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.tdayTotalViewOfNewAd[i])),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) Math.round(viewAnalysis.tdayTotalViewOfNewAd[i] / (double) viewAnalysis.tdayNumOfNewAd[i]))),
                            new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                            getCellData(i, new Color(0, 0, 0, 0), true),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.tdayNumOfUpAd[i])),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.tdayTotalViewOfUpAd[i])),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) Math.round(viewAnalysis.tdayTotalViewOfUpAd[i] / (double) viewAnalysis.tdayNumOfUpAd[i])))
                    )));
                } else if (viewAnalysis.tdayTotalViewOfNewAd[i] == 0 && viewAnalysis.tdayTotalViewOfUpAd[i] != 0) {
                    rData.add(new RowData().setValues(Arrays.asList(
                            getCellData(i, new Color(0, 0, 0, 0), true),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                            getCellData(i, new Color(0, 0, 0, 0), true),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.tdayNumOfUpAd[i])),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.tdayTotalViewOfUpAd[i])),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) Math.round(viewAnalysis.tdayTotalViewOfUpAd[i] / (double) viewAnalysis.tdayNumOfUpAd[i])))
                    )));
                } else if (viewAnalysis.tdayTotalViewOfNewAd[i] != 0 && viewAnalysis.tdayTotalViewOfUpAd[i] == 0) {
                    rData.add(new RowData().setValues(Arrays.asList(
                            getCellData(i, new Color(0, 0, 0, 0), true),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.tdayNumOfNewAd[i])),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.tdayTotalViewOfNewAd[i])),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) Math.round(viewAnalysis.tdayTotalViewOfNewAd[i] / (double) viewAnalysis.tdayNumOfNewAd[i]))),
                            new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                            getCellData(i, new Color(0, 0, 0, 0), true),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0))
                    )));
                } else if (viewAnalysis.tdayTotalViewOfNewAd[i] == 0 && viewAnalysis.tdayTotalViewOfUpAd[i] == 0) {
                    rData.add(new RowData().setValues(Arrays.asList(
                            getCellData(i, new Color(0, 0, 0, 0), true),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                            getCellData(i, new Color(0, 0, 0, 0), true),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0))
                    )));
                }
            }
        }
        rData.add(new RowData().setValues(Arrays.asList(
                getCellData("За всё время", new Color(239, 239, 239, 255), true),
                getCellData("=СУММ(B7:B40)", new Color(239, 239, 239, 255)),
                getCellData("=СУММ(C7:C40)", new Color(239, 239, 239, 255)),
                getCellData("=СУММ(D7:D40)", new Color(239, 239, 239, 255)),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                getCellData("За всё время", new Color(239, 239, 239, 255), true),
                getCellData("=СУММ(G7:G40)", new Color(239, 239, 239, 255)),
                getCellData("=СУММ(H7:H40)", new Color(239, 239, 239, 255)),
                getCellData("=СУММ(I7:I40)", new Color(239, 239, 239, 255))
        )));
        rData.add(new RowData());

        // За вчера ( По часам )
        rData.add(new RowData().setValues(Arrays.asList(
                getCellData("Вчера", new Color(0, 0, 0, 0), true),
                getCellData(viewAnalysis.tenDays.get(1), new Color(0, 0, 0, 0)),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                getCellData("Вчера", new Color(0, 0, 0, 0), true),
                getCellData(viewAnalysis.tenDays.get(1), new Color(0, 0, 0, 0))
        )));
        rData.add(new RowData().setValues(Arrays.asList(
                getCellData("Всего просмотров за вчерашний день на Новых объ-ях", new Color(239, 239, 239, 255), true),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                getCellData("Всего просмотров за вчерашний день на Поднятых объ-ях", new Color(239, 239, 239, 255), true)
        )));
        rData.add(new RowData().setValues(Arrays.asList(
                getCellData("Часовые интервалы", new Color(207, 226, 243, 255), true),
                getCellData("Кол-во объявлений", new Color(207, 226, 243, 255), true),
                getCellData("Кол-во просмотров", new Color(207, 226, 243, 255), true),
                getCellData("Сред. просмотров", new Color(207, 226, 243, 255), true),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                getCellData("Часовые интервалы", new Color(207, 226, 243, 255), true),
                getCellData("Кол-во объявлений", new Color(207, 226, 243, 255), true),
                getCellData("Кол-во просмотров", new Color(207, 226, 243, 255), true),
                getCellData("Сред. просмотров", new Color(207, 226, 243, 255), true)
        )));

        for (int i = 0; i < 24; i++) {
            {
                if (viewAnalysis.ydayTotalViewOfNewAd[i] != 0 && viewAnalysis.ydayTotalViewOfUpAd[i] != 0) {
                    rData.add(new RowData().setValues(Arrays.asList(
                            getCellData(i, new Color(0, 0, 0, 0), true),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.ydayNumOfNewAd[i])),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.ydayTotalViewOfNewAd[i])),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) Math.round(viewAnalysis.ydayTotalViewOfNewAd[i] / (double) viewAnalysis.ydayNumOfNewAd[i]))),
                            new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                            getCellData(i, new Color(0, 0, 0, 0), true),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.ydayNumOfUpAd[i])),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.ydayTotalViewOfUpAd[i])),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) Math.round(viewAnalysis.ydayTotalViewOfUpAd[i] / (double) viewAnalysis.ydayNumOfUpAd[i])))
                    )));
                } else if (viewAnalysis.ydayTotalViewOfNewAd[i] == 0 && viewAnalysis.ydayTotalViewOfUpAd[i] != 0) {
                    rData.add(new RowData().setValues(Arrays.asList(
                            getCellData(i, new Color(0, 0, 0, 0), true),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                            getCellData(i, new Color(0, 0, 0, 0), true),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.ydayNumOfUpAd[i])),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.ydayTotalViewOfUpAd[i])),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) Math.round(viewAnalysis.ydayTotalViewOfUpAd[i] / (double) viewAnalysis.ydayNumOfUpAd[i])))
                    )));
                } else if (viewAnalysis.ydayTotalViewOfNewAd[i] != 0 && viewAnalysis.ydayTotalViewOfUpAd[i] == 0) {
                    rData.add(new RowData().setValues(Arrays.asList(
                            getCellData(i, new Color(0, 0, 0, 0), true),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.ydayNumOfNewAd[i])),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) viewAnalysis.ydayTotalViewOfNewAd[i])),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) Math.round(viewAnalysis.ydayTotalViewOfNewAd[i] / (double) viewAnalysis.ydayNumOfNewAd[i]))),
                            new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                            getCellData(i, new Color(0, 0, 0, 0), true),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0))
                    )));
                } else if (viewAnalysis.ydayTotalViewOfNewAd[i] == 0 && viewAnalysis.ydayTotalViewOfUpAd[i] == 0) {
                    rData.add(new RowData().setValues(Arrays.asList(
                            getCellData(i, new Color(0, 0, 0, 0), true),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                            getCellData(i, new Color(0, 0, 0, 0), true),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0)),
                            new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue((double) 0))
                    )));
                }
            }
        }
        rData.add(new RowData().setValues(Arrays.asList(
                getCellData("За всё время", new Color(239, 239, 239, 255), true),
                getCellData("=СУММ(B46:B69)", new Color(239, 239, 239, 255)),
                getCellData("=СУММ(C46:C69)", new Color(239, 239, 239, 255)),
                getCellData("=СУММ(D46:D69)", new Color(239, 239, 239, 255)),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                getCellData("За всё время", new Color(239, 239, 239, 255), true),
                getCellData("=СУММ(G46:G69)", new Color(239, 239, 239, 255)),
                getCellData("=СУММ(H46:H69)", new Color(239, 239, 239, 255)),
                getCellData("=СУММ(I46:I69)", new Color(239, 239, 239, 255))
        )));

        sheet.setMerges(Arrays.asList(
                generateRange(1, 0, 1, 0, 2),
                generateRange(1, 0, 1, 2, 4),
                generateRange(1, 0, 1, 5, 7),
                generateRange(1, 0, 1, 7, 9),
                generateRange(1, 14, 15, 0, 4),
                generateRange(1, 14, 15, 5, 9)
        ));

        // -------------------- SET VALUES ( END ) --------------------

        gridData.setRowData(rData);
        sheet.setData(gData);
        return sheet;
    }

//    private static Sheet getSortSheet(String title, String formula, ReportFilter filters) {
//        Sheet sheet = new Sheet();
//
//        SheetProperties sheetProperties = new SheetProperties();
//        sheetProperties.setTitle(title);
//        GridProperties gridProperties = new GridProperties();
//        gridProperties.setFrozenRowCount(1);
//        sheetProperties.setGridProperties(gridProperties);
//        sheet.setProperties(sheetProperties);
//
//        List<GridData> gData = new ArrayList<>();
//        GridData gridData = new GridData();
//        gData.add(gridData);
//
//        List<RowData> rData = new ArrayList<>();
//
//        // -------------------- SET HEADERS --------------------
//        rData.add(getRowHeaders(filters));
//
//        // -------------------- SET VALUES --------------------
//        RowData rowVal = new RowData();
//        List<CellData> clValues = new ArrayList<>();
//
//        CellData cell = new CellData();
//
//        ExtendedValue exValue = new ExtendedValue();
//        exValue.setFormulaValue(formula);
//        cell.setUserEnteredValue(exValue);
//        clValues.add(cell);
//
//        rowVal.setValues(clValues);
//        rData.add(rowVal);
//        // -------------------- SET VALUES ( END ) --------------------
//
//        gridData.setRowData(rData);
//        sheet.setData(gData);
//        return sheet;
//    }

    private static RowData getRowHeaders(ReportFilter filters) {
        RowData rowData = new RowData();
        List<CellData> clHeaders = new ArrayList<>();
        Color headerBgColor = new Color(201, 218, 248);


        if (filters.isPosition()) {
            CellData header0 = getCellData("Позиция", headerBgColor, true);
            header0.setNote("Позиция в выдаче на Авито в момент парсинга");
            clHeaders.add(header0);
        }
        // Пример добавление заметок
        CellData header = getCellData("Заголовок", headerBgColor, true);
        header.setNote("Заголовок объявления");
        clHeaders.add(header);
        CellData header1 = getCellData("Цена", headerBgColor, true);
        header1.setNote("Цена товара/услуги на момент парсинга");
        clHeaders.add(header1);
        CellData header01 = getCellData("Пониж.Цена", headerBgColor, true);
        header01.setNote("\"1\" - означает наличие знака понижения цены, продавец снизил цену.\n" +
                "\"0\" - означает отсутствие такого знака, цена не снижалась");
        clHeaders.add(header01);
        CellData header2 = getCellData("Просм. Всего", headerBgColor, true);
        header2.setNote("Общее количество просмотров на данном объявлении в момент парсинга.");
        clHeaders.add(header2);
        CellData header3 = getCellData("Просм. Сегодня", headerBgColor, true);
        header3.setNote("Количество просмотров на данном объявлении за сегодняшний день,\n" +
                "с 00:00 часов до момента парсинга.");
        clHeaders.add(header3);
        CellData header03 = getCellData("Просм. Вчера", headerBgColor, true);
        header03.setNote("Количество просмотров на данном объявлении за весь вчерашний день, \n" +
                "с 00:00 до 24:00 часов");
        clHeaders.add(header03);
        if (filters.isDate()) {
            CellData header15 = getCellData("Просм. 10 дней", headerBgColor, true);
            header15.setNote("Общее количество просмотров за предыдущие 10 дней, включая сегодняшний");
            clHeaders.add(header15);
            CellData header16 = getCellData("max. Просм. 10дн.", headerBgColor, true);
            header16.setNote("Максимальное количество просмотров за один из предыдущих 10 дней");
            clHeaders.add(header16);
            CellData header17 = getCellData("Дата. max. Просм.", headerBgColor, true);
            header17.setNote("Тот день, когда данное объявление набрало максимальное кол-во просмотров за период предыдущих 10 дней.");
            clHeaders.add(header17);
            CellData header18 = getCellData("Просм. ср. 10 дней", headerBgColor, true);
            header18.setNote("Среднее количество просмотров за предыдущие 10 дней.");
            clHeaders.add(header18);
        }
        CellData header4 = getCellData("Методы продвижения", headerBgColor, true);
        header4.setNote("Методы продвижения (платные услуги)\n" +
                "1 - Премиум\n" +
                "2 - VIP\n" +
                "3 - Выделение\n" +
                "4 - Поднятие\n" +
                "5 - XL");
        clHeaders.add(header4);
        CellData header6 = getCellData("Время поднятия", headerBgColor, true);
        header6.setNote("Дата и время крайнего поднятия объявления");
        clHeaders.add(header6);

        if (filters.isDate()) {
            CellData header14 = getCellData("Дата Создания", headerBgColor, true);
            header14.setNote("Дата создания объявления");
            clHeaders.add(header14);
        }

        if (filters.isPhoto()) {
            CellData header8 = getCellData("Фото (шт)", headerBgColor, true);
            header8.setNote("Количество фотографий размещеных в данном объявлении");
            clHeaders.add(header8);
        }


        if (filters.isDescription()) {
            CellData header9 = getCellData("Текст", headerBgColor, true);
            header9.setNote("Текст (описание) объявления");
            clHeaders.add(header9);
        }

        if (filters.isDescriptionLength()) {
            CellData header10 = getCellData("Кол-во знаков", headerBgColor, true);
            header10.setNote("Количество знаков (символов) в тексте объявления");
            clHeaders.add(header10);
        }

        CellData header05 = getCellData("Доставка", headerBgColor, true);
        header05.setNote("\"1\" - Возможна доставка\n" +
                "\"0\" - Продавец не указал о возможной доставке");
        clHeaders.add(header05);

        if (filters.isSellerName()) {
            CellData header11 = getCellData("Имя продавца", headerBgColor, true);
            clHeaders.add(header11);

        }

        CellData header12 = getCellData("ID продавца", headerBgColor, true);
        clHeaders.add(header12);
        if (filters.isPhone()) {
            CellData header13 = getCellData("Телефон", headerBgColor, true);
            clHeaders.add(header13);
        }

        CellData header10 = getCellData("Активных объявлений", headerBgColor, true);
        header10.setNote("Количество Активных объявлений данного продавца, размещенных на Авито");
        clHeaders.add(header10);

        CellData header5 = getCellData("Адрес", headerBgColor, true);
        clHeaders.add(header5);

        CellData header7 = getCellData("Ссылка", headerBgColor, true);
        clHeaders.add(header7);


        rowData.setValues(clHeaders);
        return rowData;
    }

    private static CellData getCellData(Object val) {
        return getCellData(val, new Color(255, 255, 255));
    }

    private static CellData getCellData(Object val, java.awt.Color userColor) {
        return getCellData(val, userColor, false);
    }

    private static CellData getCellData(Object val, Color userColor, Boolean isBold) {
        CellData cell = new CellData();


        CellFormat format = new CellFormat();
        com.google.api.services.sheets.v4.model.Color color = new com.google.api.services.sheets.v4.model.Color();

        userColor = convertRgba(userColor);
        color.setRed((float) userColor.getRed() / 255);
        color.setGreen((float) userColor.getGreen() / 255);
        color.setBlue((float) userColor.getBlue() / 255);

        format.setBackgroundColor(color);
        if (isBold)
            format.setTextFormat(new TextFormat().setBold(true));

        if (userColor.getRGB() < -70000)
            cell.setUserEnteredFormat(format);

        ExtendedValue exValue = new ExtendedValue();
        if (val instanceof Integer) {
            exValue.setNumberValue(Double.valueOf((Integer) val));
        } else if (val instanceof Double) {
            exValue.setNumberValue(Math.round((Double) val * 100.0) / 100.0);
        } else if (val instanceof String) {
            String sVal = (String) val;
            if (sVal.startsWith("="))
                exValue.setFormulaValue(sVal);
            else
                exValue.setStringValue(sVal);
        } else {
            exValue.setStringValue("");
        }

        cell.setUserEnteredValue(exValue);

        return cell;
    }

    private static void setPermission(String fileId, boolean offlineMod) throws IOException {
        BatchRequest batch = driveService.batch();

        Permission userPermission = new Permission()
                .setType("anyone")
                .setRole("writer");

        driveService.permissions().create(fileId, userPermission)
                .setFields("id")
                .queue(batch, new JsonBatchCallback<Permission>() {
                    @Override
                    public void onSuccess(Permission permission, HttpHeaders httpHeaders) {
                    }

                    @Override
                    public void onFailure(GoogleJsonError googleJsonError, HttpHeaders httpHeaders) {
                    }
                });
        batch.execute();

        if (offlineMod) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            driveService.files().export(fileId, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .executeMediaAndDownloadTo(byteArrayOutputStream);

            try (OutputStream outputStream = new FileOutputStream("reports/" + fileId + ".xlsx")) {
                byteArrayOutputStream.writeTo(outputStream);
            }
        }
    }

    private static Drive createDriveService() throws IOException {
        GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(KEY_FILE), HTTP_TRANSPORT, JSON_FACTORY);
        credential = credential.createScoped(Collections.singleton(DriveScopes.DRIVE_FILE));

        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private static Sheets createSheetsService() throws IOException {
        GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(KEY_FILE), HTTP_TRANSPORT, JSON_FACTORY);
        credential = credential.createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}