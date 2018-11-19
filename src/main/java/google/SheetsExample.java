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
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.*;
import parser.Ad;

import java.awt.*;
import java.awt.Color;
import java.io.*;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static boolean isNum(String s) {
        try {
            Integer.parseInt(s.replaceAll(" ", ""));
            return true;
        } catch (Exception e) {
            return false;
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
        if (filters.isDescription()) {
            for (Ad ad : ads)
                descLength += ad.getText() == null ? 0 : ad.getText().length();

            offlineMod = descLength > 3_000_000;
        }


        List<Integer> viewsMax = new ArrayList<Integer>();
        List<Integer> dailyViewsMax = new ArrayList<Integer>();
        List<Integer> viewsTenDayMax = new ArrayList<Integer>();
        List<Integer> maxTenDayMax = new ArrayList<Integer>();
        List<Integer> viewsAverageTenDayMax = new ArrayList<Integer>();
        for (int i = 0; i < ads.size(); i++) {
            Ad ad = ads.get(i);
            if (isNum(ad.getViews())) {
                viewsMax.add(Integer.parseInt(ad.getViews()));
            }
            if (isNum(ad.getDailyViews())) {
                dailyViewsMax.add(Integer.parseInt(ad.getDailyViews()));
            }
            if (isNum(ad.getViewsTenDay())) {
                viewsTenDayMax.add(Integer.parseInt(ad.getViewsTenDay()));
            }

            Integer maxTenDay = ad.getMaxTenDay();
            if (maxTenDay != null)
                maxTenDayMax.add(maxTenDay);

            if (isNum(ad.getViewsAverageTenDay())) {
                viewsAverageTenDayMax.add(Integer.parseInt(ad.getViewsAverageTenDay()));
            }
        }
        int maxViews = Collections.max(viewsMax);
        int minViews = Collections.min(viewsMax);
        int maxDailyViews = Collections.max(dailyViewsMax);
        int minDailyViews = Collections.min(dailyViewsMax);
        int maxViewsTenDay = Collections.max(viewsTenDayMax);
        int minViewsTenDay = Collections.min(viewsTenDayMax);
        int maxMaxTenDay = Collections.max(maxTenDayMax);
        int minMaxTenDay = Collections.min(maxTenDayMax);
        int maxViewsAverageTenDay = Collections.max(viewsAverageTenDayMax);
        int minViewsAverageTenDay = Collections.min(viewsAverageTenDayMax);


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

                RowData rowVal = new RowData();
                List<CellData> clValues = new ArrayList<>();

                if (filters.isPosition()) {
                    Integer position = 0;
                    try {
                        position = ad.getPosition();
                    } catch (Exception ignore) { }

                    double coff = (position / 00.7) / 100;
                    int alpha = (int) (255 * (coff > 1 ? 0 : (1 - coff)));
                    clValues.add(getCellData(position, new Color(183,225,205, alpha)));
                }

                String titleName = "";
                try {
                    titleName = ad.getTitle();
                } catch (Exception ignore) {
                }
                if (titleName.isEmpty()) continue;
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

                String views = "";
                try {
                    views = ad.getViews();

                    double coff = (Integer.parseInt(views) / ((maxViews - minViews) / 100.0)) / 100;
                    int alpha = (int) (255 * coff);

                    clValues.add(getCellData(Integer.parseInt(views), new Color(255,214,102, alpha)));
                } catch (Exception ignore) {
                    clValues.add(getCellData(0));
                }


                String dailyViews = "";
                try {
                    dailyViews = ad.getDailyViews();
                    double coff = (Integer.parseInt(dailyViews) / ((maxDailyViews - minDailyViews) / 100.0)) / 100;
                    int alpha = (int) (255 * coff);
                    clValues.add(getCellData(Integer.parseInt(dailyViews), new Color(147,196,125, alpha)));
                } catch (Exception ignore) {
                    clValues.add(getCellData(0));
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
                } catch (Exception ignore) {
                }
                clValues.add(getCellData(services));

                String address = "";
                try {
                    address = ad.getAddress();
                } catch (Exception ignore) {
                }
                clValues.add(getCellData(address));


                String data = "";
                try {
                    data = ad.getData();
                } catch (Exception ignore) {
                }
                clValues.add(getCellData(data));

                String url = "";
                try {
                    url = ad.getUrl();
                } catch (Exception ignore) {
                }
                clValues.add(getCellData(url));

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
                    sellerId = ad.getSellerId();
                } catch (Exception ignore) {
                }
                clValues.add(getCellData(sellerId));


                if (filters.isPhone()) {
                    String phone = "";
                    try {
                        phone = ad.getPhone();
                    } catch (Exception ignore) {
                    }
                    clValues.add(getCellData(phone));
                }




                if (filters.isDate()) {
                    if (ad.hasStats() != null && ad.hasStats()) {
                        String dateApplication = "";
                        try {
                            dateApplication = ad.getDateApplication();
                        } catch (Exception ignore) {
                        }
                        clValues.add(getCellData(dateApplication));

                        try {
                            String viewsTenDay = ad.getViewsTenDay();

                            double coff = (Integer.parseInt(viewsTenDay) / ((maxViewsTenDay - minViewsTenDay) / 100.0)) / 100;
                            int alpha = (int) (255 * coff);
                            clValues.add(getCellData(Integer.parseInt(viewsTenDay),new Color(194,123,160, alpha)));
                        } catch (Exception ignore) {
                            clValues.add(getCellData(0));
                        }

                        try {
                            Integer maxTenDay = ad.getMaxTenDay();
                            double coff = (maxTenDay / ((maxMaxTenDay - minMaxTenDay) / 100.0)) / 100;
                            int alpha = (int) (255 * coff);
                            clValues.add(getCellData(maxTenDay,new Color(87,187,138, alpha)));
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
                            double coff = (Integer.parseInt(viewsAverageTenDay) / ((maxViewsAverageTenDay - minViewsAverageTenDay) / 100.0)) / 100;
                            int alpha = (int) (255 * coff);
                            clValues.add(getCellData(Integer.parseInt(viewsAverageTenDay),new Color(234,153,153, alpha)));
                        } catch (Exception ignore) {
                            clValues.add(getCellData(0));
                        }

                    } else {
                        clValues.add(getCellData(new SimpleDateFormat("yyyy.MM.dd").format(new Date())));

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

                rowVal.setValues(clValues);
                rData.add(rowVal);
            }
            // -------------------- SET VALUES ( END ) --------------------

            gridData.setRowData(rData);
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
            sheets.add(getStatisticSheet(ads));

            requestBody.setSheets(sheets);
            Sheets.Spreadsheets.Create request = sheetsService.spreadsheets().create(requestBody);

            Spreadsheet response = null;
            for (int i = 0; i < 3; i++) {
                try {
                    response = request.execute();
                    break;
                } catch (Exception e) {
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
            } else
                return response.getSpreadsheetUrl();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Не удалось сформировать отчёт");
            e.printStackTrace();
            throw new Exception((e.getMessage().contains("Google API") ?
                    "Не удалось получить ответ от Google API" :
                    "Не удалось сформировать отчёт"));
        }
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

    private static Sheet getStatisticSheet(List<Ad> ads) {
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
                new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЗ('Объявления'!A2:A20000)"))
        )));
        rData.add(new RowData());

        rData.add(new RowData().setValues(Arrays.asList(
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Методы продвижения:")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("4 - Поднятие")),
                new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!E2:E20000;\"*4*\")"))
        )));

        rData.add(new RowData().setValues(Arrays.asList(
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("3 - Выделение")),
                new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!E2:E20000;\"*3*\")"))
        )));

        rData.add(new RowData().setValues(Arrays.asList(
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("2 - VIP")),
                new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!E2:E20000;\"*2*\")"))
        )));

        rData.add(new RowData().setValues(Arrays.asList(
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("1 - Премиум")),
                new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!E2:E20000;\"*1*\")"))
        )));
        rData.add(new RowData());

        rData.add(new RowData().setValues(Arrays.asList(
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Цена:")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Минимальная:")),
                new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=МИН('Объявления'!B2:B20000)"))
        )));

        rData.add(new RowData().setValues(Arrays.asList(
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Средняя:")),
                new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СРЗНАЧ('Объявления'!B2:B20000)"))
        )));

        rData.add(new RowData().setValues(Arrays.asList(
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Максимальная:")),
                new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=МАКС('Объявления'!B2:B20000)"))
        )));
        rData.add(new RowData());

        rData.add(new RowData().setValues(Arrays.asList(
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Просмотры:")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Минимальная:")),
                new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=МИН('Объявления'!C2:C20000)"))
        )));

        rData.add(new RowData().setValues(Arrays.asList(
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Средняя:")),
                new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СРЗНАЧ('Объявления'!C2:C20000)"))
        )));

        rData.add(new RowData().setValues(Arrays.asList(
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Максимальная:")),
                new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=МАКС('Объявления'!C2:C20000)"))
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
                rData.add(new RowData().setValues(Arrays.asList(
                        new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Районы:")),
                        new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(address)),
                        new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!F2:F20000;\"*" + address + "*\")"))
                )));
                coin = false;
            } else {
                rData.add(new RowData().setValues(Arrays.asList(
                        new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                        new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(address)),
                        new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!F2:F20000;\"*" + address + "*\")"))
                )));
            }
        }
        // -------------------- SET VALUES ( END ) --------------------

        gridData.setRowData(rData);
        sheet.setData(gData);
        return sheet;
    }

    private static Sheet getSortSheet(String title, String formula, ReportFilter filters) {
        Sheet sheet = new Sheet();

        SheetProperties sheetProperties = new SheetProperties();
        sheetProperties.setTitle(title);
        GridProperties gridProperties = new GridProperties();
        gridProperties.setFrozenRowCount(1);
        sheetProperties.setGridProperties(gridProperties);
        sheet.setProperties(sheetProperties);

        List<GridData> gData = new ArrayList<>();
        GridData gridData = new GridData();
        gData.add(gridData);

        List<RowData> rData = new ArrayList<>();

        // -------------------- SET HEADERS --------------------
        rData.add(getRowHeaders(filters));

        // -------------------- SET VALUES --------------------
        RowData rowVal = new RowData();
        List<CellData> clValues = new ArrayList<>();

        CellData cell = new CellData();

        ExtendedValue exValue = new ExtendedValue();
        exValue.setFormulaValue(formula);
        cell.setUserEnteredValue(exValue);
        clValues.add(cell);

        rowVal.setValues(clValues);
        rData.add(rowVal);
        // -------------------- SET VALUES ( END ) --------------------

        gridData.setRowData(rData);
        sheet.setData(gData);
        return sheet;
    }

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
        CellData header2 = getCellData("Просм. Всего", headerBgColor, true);
        header2.setNote("Общее количество просмотров на данном объявлении в момент парсинга.");
        clHeaders.add(header2);
        CellData header3 = getCellData("Просм. Сегодня", headerBgColor, true);
        header3.setNote("Количество просмотров на данном объявлении за сегодняшний день,\n" +
                "с 00:00 часов до момента парсинга.");
        clHeaders.add(header3);
        CellData header4 = getCellData("Методы продвижения", headerBgColor, true);
        header4.setNote("Методы продвижения (платные услуги)\n" +
                "1 - Премиум\n" +
                "2 - VIP\n" +
                "3 - Выделение\n" +
                "4 - Поднятие\n" +
                "5 - XL");
        clHeaders.add(header4);
        CellData header5 = getCellData("Адрес", headerBgColor, true);
        clHeaders.add(header5);
        CellData header6 = getCellData("Время поднятия (Переделать)", headerBgColor, true);
        header6.setNote("Дата и время крайнего поднятия объявления");
        clHeaders.add(header6);
        CellData header7 = getCellData("Ссылка", headerBgColor, true);
        clHeaders.add(header7);


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

        if (filters.isDate()) {
            CellData header14 = getCellData("Дата Создания", headerBgColor, true);
            header14.setNote("Дата создания объявления");
            clHeaders.add(header14);
            CellData header15 = getCellData("Просм. 10 дней", headerBgColor, true);
            header15.setNote("Общее количество просмотров за предыдущие 10 дней, включая сегодняшний");
            clHeaders.add(header15);
            CellData header16 = getCellData("max. Просм. 10дн.", headerBgColor, true);
            header16.setNote("Максимальное количество просмотров за один из предыдущих 10 дней");
            clHeaders.add(header16);
            CellData header17 = getCellData("Дата. max. Просм.", headerBgColor, true);
            clHeaders.add(header17);
            CellData header18 = getCellData("Просм. ср. 10 дней", headerBgColor, true);
            header18.setNote("Среднее количество просмотров за предыдущие 10 дней.");
            clHeaders.add(header18);
        }

        rowData.setValues(clHeaders);
        return rowData;
    }

    private static CellData getCellData(Object val) {
        return getCellData(val, new Color(255, 255, 255));
    }

    private static CellData getCellData(Object val,  java.awt.Color userColor) {
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

        cell.setUserEnteredFormat(format);

        ExtendedValue exValue = new ExtendedValue();
        if (val instanceof Integer) {
            exValue.setNumberValue(Double.valueOf((Integer) val));
        } else if (val instanceof String) {
            exValue.setStringValue((String) val);
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
}