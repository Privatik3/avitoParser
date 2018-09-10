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

import java.io.*;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

    public static String generateSheet(String title, List<Ad> ads, ReportFilter filters) {

        int descLength = 0;
        for (Ad ad : ads)
            descLength += ad.getText() == null ? 0 : ad.getText().length();

        boolean offlineMod = descLength > 50;

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

                String titleName = "";
                try {
                    titleName = ad.getTitle();
                } catch (Exception ignore) {
                }
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
                    clValues.add(getCellData(Integer.parseInt(views)));
                } catch (Exception ignore) {
                    clValues.add(getCellData(0));
                }


                String dailyViews = "";
                try {
                    dailyViews = ad.getDailyViews();
                    clValues.add(getCellData(Integer.parseInt(dailyViews)));
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
                    } catch (Exception ignore) { }
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


                if (filters.isPosition()) {
                    Integer position = 0;
                    try {
                        position = ad.getPosition();
                    } catch (Exception ignore) {
                    }
                    clValues.add(getCellData(position));
                }

                if (filters.isDate()) {
                    if (ad.hasStats()) {
                        String dateApplication = "";
                        try {
                            dateApplication = ad.getDateApplication();
                        } catch (Exception ignore) {
                        }
                        clValues.add(getCellData(dateApplication));

                        try {
                            String viewsTenDay = ad.getViewsTenDay();
                            clValues.add(getCellData(Integer.parseInt(viewsTenDay)));
                        } catch (Exception ignore) {
                            clValues.add(getCellData(0));
                        }

                        try {
                            Integer maxTenDay = ad.getMaxTenDay();
                            clValues.add(getCellData(maxTenDay));
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
                            clValues.add(getCellData(Integer.parseInt(viewsAverageTenDay)));
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
            sheets.add(getSortSheet("Цены (сорт)", "=SORT('Объявления'!A2:U20000,2,FALSE)", filters));
            sheets.add(getSortSheet("Просм. Всего", "=SORT('Объявления'!A2:U20000;3;FALSE)", filters));
            sheets.add(getSortSheet("Методы (сорт)", "=SORT('Объявления'!A2:U20000;5;FALSE)", filters));
            sheets.add(getSortSheet("Просм. За день", "=SORT('Объявления'!A2:U20000;4;FALSE)", filters));
            if (filters.isDate()) {
                sheets.add(getSortSheet("Просм. 10 дней", "=SORT('Объявления'!A2:U20000;17;FALSE)", filters));
                sheets.add(getSortSheet("max. Просм. 10дн.", "=SORT('Объявления'!A2:U20000;18;FALSE)", filters));
                sheets.add(getSortSheet("Просм. ср. 10 дней", "=SORT('Объявления'!A2:U20000;20;FALSE)", filters));
            }


            // -------------------- STATISTIC SHEET --------------------
            sheets.add(getStatisticSheet(ads));

            requestBody.setSheets(sheets);
            Sheets.Spreadsheets.Create request = sheetsService.spreadsheets().create(requestBody);

            Spreadsheet response = request.execute();

            // 2. PUBLISH SPREADSHEAT VIA DRIVE API
            String fileId = response.getSpreadsheetId();
            setPermission(fileId, offlineMod);

            if (offlineMod) {
                updateDescTable(ads, fileId);

                InetAddress localHost = InetAddress.getLocalHost();
                return String.format("http://%s:8081/api/report.xlsx?fileID=%s", localHost.getHostAddress(), fileId);
            }
            else
                return response.getSpreadsheetUrl();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Не удалось сформировать отчёт");
            log.log(Level.SEVERE, "Exception: " + e.getMessage());
            log.log(Level.SEVERE, "Method: " + e.getStackTrace()[0].getMethodName());
            log.log(Level.SEVERE, "Line: " + e.getStackTrace()[0].getLineNumber());

            e.printStackTrace();
        }

       return "";
    }

    public static void updateDescTable(List<Ad> ads, String fileId) throws IOException {

        XSSFWorkbook wb = new XSSFWorkbook( new FileInputStream("reports/" + fileId + ".xlsx") );

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
                });
            }

            for (Integer i = 1 ; i < wb.getSheetAt(0).getLastRowNum() ; i++) {
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

        clHeaders.add(getCellData("Заголовок"));
        clHeaders.add(getCellData("Цена"));
        clHeaders.add(getCellData("Просм. Всего"));
        clHeaders.add(getCellData("Просм. За день"));
        clHeaders.add(getCellData("Методы продвижения"));
        clHeaders.add(getCellData("Адрес"));
        clHeaders.add(getCellData("Дата"));
        clHeaders.add(getCellData("Ссылка"));
        if (filters.isPhoto()) {
            clHeaders.add(getCellData("Фото (шт)"));
        }
        if (filters.isDescription()) {
            clHeaders.add(getCellData("Текст"));
        }
        if (filters.isDescriptionLength()) {
            clHeaders.add(getCellData("Кол-во знаков"));
        }
        if (filters.isSellerName()) {
            clHeaders.add(getCellData("Имя продавца"));
        }
        clHeaders.add(getCellData("ID продавца"));
        if (filters.isPhone()) {
            clHeaders.add(getCellData("Телефон"));
        }
        if (filters.isPosition()) {
            clHeaders.add(getCellData("Позиция в выдаче"));
        }
        if (filters.isDate()) {
            clHeaders.add(getCellData("Дата Создания"));
            clHeaders.add(getCellData("Просм. 10 дней"));
            clHeaders.add(getCellData("max. Просм. 10дн."));
            clHeaders.add(getCellData("Дата. max. Просм."));
            clHeaders.add(getCellData("Просм. ср. 10 дней"));
        }


        for (CellData cell : clHeaders) {
            CellFormat format = new CellFormat();
            Color color = new Color();

            color.setRed((float) 201 / 255);
            color.setGreen((float) 218 / 255);
            color.setBlue((float) 248 / 255);

            format.setBackgroundColor(color);
            format.setTextFormat(new TextFormat().setBold(true));

            cell.setUserEnteredFormat(format);
        }

        rowData.setValues(clHeaders);
        return rowData;
    }

    private static CellData getCellData(Integer val) {
        CellData cell = new CellData();

        ExtendedValue exValue = new ExtendedValue();
        exValue.setNumberValue(Double.valueOf(val));
        cell.setUserEnteredValue(exValue);

        return cell;
    }

    private static CellData getCellData(String val) {
        CellData cell = new CellData();

        ExtendedValue exValue = new ExtendedValue();
        exValue.setStringValue(val);
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