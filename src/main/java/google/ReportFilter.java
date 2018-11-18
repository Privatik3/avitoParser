package google;

import java.io.Serializable;

public class ReportFilter implements Serializable {

    private boolean photo;
    private boolean description;
    private boolean descriptionLength;
    private boolean sellerName;
    private boolean position;
    private boolean date;
    private boolean phone;

    public boolean isPhoto() {
        return photo;
    }

    public void setPhoto(boolean photo) {
        this.photo = photo;
    }

    public boolean isDescription() {
        return description;
    }

    public void setDescription(boolean description) {
        this.description = description;
    }

    public boolean isDescriptionLength() {
        return descriptionLength;
    }

    public void setDescriptionLength(boolean descriptionLength) {
        this.descriptionLength = descriptionLength;
    }

    public boolean isSellerName() {
        return sellerName;
    }

    public void setSellerName(boolean sellerName) {
        this.sellerName = sellerName;
    }

    public boolean isPosition() {
        return position;
    }

    public void setPosition(boolean position) {
        this.position = position;
    }

    public boolean isDate() {
        return date;
    }

    public void setDate(boolean date) {
        this.date = date;
    }

    public boolean isPhone() {
        return phone;
    }

    public void setPhone(boolean phone) {
        this.phone = phone;
    }
}
