package api;

public enum RecordType {
    GOOGLE_DOCS,
    EXCEL,
    NAN;

    public static RecordType getType(String name) {
        switch (name) {
            case "EXCEL": return RecordType.EXCEL;
            case "GOOGLE_DOCS": return RecordType.GOOGLE_DOCS;
        }

        return RecordType.NAN;
    }
}
