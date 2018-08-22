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
import parser.Ad;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SheetsExample {

    private static Drive driveService;
    private static Sheets sheetsService;

    private static HttpTransport HTTP_TRANSPORT;
    private static JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static String KEY_FILE = "olx-parser.json";
    private static String APPLICATION_NAME = "OLX Parser";

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            sheetsService = createSheetsService();
            driveService = createDriveService();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String generateSheet(String title, List<Ad> ads, ReportFilter filters) throws IOException {

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
            } catch (Exception ignore) {}
            clValues.add(getCellData(titleName));

            try {
                String price = ad.getPrice();
                   try {
                       int priceInt = Integer.parseInt(price.replaceAll(" " , ""));
                       clValues.add(getCellData(priceInt));
                   }  catch (Exception ignored) {
                       clValues.add(getCellData(price));
                   }
            } catch (Exception ignore) {}

            String views = "";
            try {
                views = ad.getViews();
            } catch (Exception ignore) {}
            clValues.add(getCellData(Integer.parseInt(views)));

            String dailyViews = "";
            try {
                dailyViews = ad.getDailyViews();
            } catch (Exception ignore) {}
            clValues.add(getCellData(dailyViews));

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
            } catch (Exception ignore) {}
            clValues.add(getCellData(services));

            String address = "";
            try {
                address = ad.getAddress();
            } catch (Exception ignore) {}
            clValues.add(getCellData(address));


            String data = "";
            try {
                data = ad.getData();
            } catch (Exception ignore) {}
            clValues.add(getCellData(data));

            String url = "";
            try {
                url = ad.getUrl();
            } catch (Exception ignore) {}
            clValues.add(getCellData(url));

            if (filters.isPhoto()) {
                String numberPictures = "";
                try {
                    numberPictures = ad.getNumberPictures();
                } catch (Exception ignore) {}
                clValues.add(getCellData(numberPictures));
            }


            if (filters.isDescription()) {
                String text = "";
                try {
                    text = ad.getText();
                } catch (Exception ignore) {}
                clValues.add(getCellData(text));
            }


            if (filters.isDescriptionLength()) {
                String quantityText = "";
                try {
                    quantityText = ad.getQuantityText();
                } catch (Exception ignore) {}
                clValues.add(getCellData(quantityText));
            }


            if (filters.isSellerName()) {
                String seller = "";
                try {
                    seller = ad.getSeller();
                } catch (Exception ignore) {}
                clValues.add(getCellData(seller));
            }

            String sellerId = "";
            try {
                sellerId = ad.getSellerId();
            } catch (Exception ignore) {}
            clValues.add(getCellData(sellerId));


            if (filters.isPhone()) {
                String phone = "";
                try {
                    phone = ad.getPhone();
                } catch (Exception ignore) {}
                clValues.add(getCellData(phone));
            }


            if (filters.isPosition()) {
                Integer position = 0;
                try {
                    position = ad.getPosition();
                } catch (Exception ignore) {}
                clValues.add(getCellData(position));
            }


            if (filters.isDate()) {
                String dateApplication = "";
                try {
                    dateApplication = ad.getDateApplication();
                } catch (Exception ignore) {
                }
                clValues.add(getCellData(dateApplication));


                if (ad.hasStats()) {
                    String viewsTenDay = "0";
                    try {
                        viewsTenDay = ad.getViewsTenDay();
                    } catch (Exception ignore) {
                    }
                    clValues.add(getCellData(Integer.parseInt(viewsTenDay)));

                    String viewsAverageTenDay = "0";
                    try {
                        viewsAverageTenDay = ad.getViewsAverageTenDay();
                    } catch (Exception ignore) {
                    }
                    clValues.add(getCellData(Integer.parseInt(viewsAverageTenDay)));
                }else {
                    clValues.add(getCellData(0));
                    clValues.add(getCellData(0));
                }
            }


            rowVal.setValues(clValues);
            rData.add(rowVal);


            /*try {
                RowData rowVal = new RowData();
                List<CellData> clValues = new ArrayList<>();

                clValues.add(getCellData(ad.getTitle()));
                try {
                    clValues.add(getCellData(Integer.parseInt(ad.getPrice())));
                } catch (Exception e) {
                    clValues.add(getCellData(ad.getPrice()));
                }

                clValues.add(getCellData(Integer.parseInt(ad.getViews().equals("") ? "0" : ad.getViews())));
                clValues.add(getCellData((ad.isTop() ? "1 " : "") + (ad.isPromoted() ? "2" : "")));
                clValues.add(getCellData(ad.getCity()));
                try {
                    clValues.add(getCellData(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(ad.getDate())));
                } catch (Exception e) {
                    clValues.add(getCellData(""));
                }
                clValues.add(getCellData(ad.getPhotos().size()));
                clValues.add(getCellData(ad.getDescription()));
                clValues.add(getCellData(ad.getDescription().length()));
                if (isPhoneEnable) {
                    ArrayList<String> phones = ad.getOwner().getPhones();

                    String phoneInfo = phones.size() > 0 ? "" : "  ";
                    for (String phone : phones)
                        phoneInfo += phone + ", ";

                    clValues.add(getCellData(phoneInfo.substring(0, phoneInfo.length() - 2)));
                }
                clValues.add(getCellData(ad.getOwner().getName()));
                clValues.add(getCellData(ad.getOwner().getId()));
                clValues.add(getCellData(ad.getOwner().getUserSince()));
                clValues.add(getCellData(ad.getUrl()));
                clValues.add(getCellData(ad.getSerialNumber()));

                rowVal.setValues(clValues);
                rData.add(rowVal);
            } catch (Exception e) {
                e.printStackTrace();
            }*/

        }
        // -------------------- SET VALUES ( END ) --------------------

        gridData.setRowData(rData);
        mainSheet.setData(gData);
        sheets.add(mainSheet);
        // -------------------- MAIN SHEET ( END ) --------------------

        // -------------------- SORTS SHEETS --------------------
        sheets.add(getSortSheet("Цены (сорт)", "=SORT('Объявления'!A2:R20000,2,FALSE)", filters));
        sheets.add(getSortSheet("\uD83D\uDC41 Всего", "=SORT('Объявления'!A2:R20000;3;FALSE)", filters));
        sheets.add(getSortSheet("Методы (сорт)", "=SORT('Объявления'!A2:R20000;5;FALSE)", filters));
        sheets.add(getSortSheet("\uD83D\uDC41 За день", "=SORT('Объявления'!A2:R20000;4;FALSE)", filters));
        if (filters.isDate()) {
            sheets.add(getSortSheet("\uD83D\uDC41 10 дней", "=SORT('Объявления'!A2:R20000;17;FALSE)", filters));
            sheets.add(getSortSheet("\uD83D\uDC41 ср. 10 дней", "=SORT('Объявления'!A2:R20000;18;FALSE)", filters));
        }


        // -------------------- STATISTIC SHEET --------------------
        sheets.add(getStatisticSheet(ads));

        requestBody.setSheets(sheets);
        Sheets.Spreadsheets.Create request = sheetsService.spreadsheets().create(requestBody);

        Spreadsheet response = request.execute();

        // TODO: Change code below to process the `response` object:
        System.out.println(response.getSpreadsheetUrl());

        // 2. PUBLISH SPREADSHEAT VIA DRIVE API
        String fileId = response.getSpreadsheetId();
        setPermission(fileId);

        return response.getSpreadsheetUrl();
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

        rData.add(new RowData().setValues(Arrays.asList(
                new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Районы:"))
        )));
        for (String address : new HashSet<>(ads.stream().map(ad -> {
            String address = "";
            if (ad.getAddress().contains("м.")) {
                address = ad.getAddress().substring(ad.getAddress().indexOf("м."));
                address = address.substring(0, address.contains(",") ? address.indexOf(",") : address.length() - 1);
            }
            return address;
        }).collect(Collectors.toList()))) {
            if (address.isEmpty()) continue;
            rData.add(new RowData().setValues(Arrays.asList(
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("")),
                    new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(address)),
                    new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue("=СЧЁТЕСЛИМН('Объявления'!F2:F20000;\"*" + address + "*\")"))
            )));
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
        clHeaders.add(getCellData("\uD83D\uDC41 Всего"));
        clHeaders.add(getCellData("\uD83D\uDC41 За день"));
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
            clHeaders.add(getCellData("Позиция в выдачу"));
        }
        if (filters.isDate()) {
            clHeaders.add(getCellData("Дата Создания"));
        }
        clHeaders.add(getCellData("\uD83D\uDC41 10 дней"));
        clHeaders.add(getCellData("\uD83D\uDC41 ср. 10 дней"));


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

    private static void setPermission(String fileId) throws IOException {
        BatchRequest batch = driveService.batch();
        /*Permission userPermission = new Permission()
                .setType("group")
                .setRole("writer")
                .setEmailAddress("olx-parser@googlegroups.com");*/

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