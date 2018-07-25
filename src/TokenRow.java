import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

class TokenRow {

    private final StringProperty tokenRow;

    TokenRow(String token) {
        this.tokenRow = new SimpleStringProperty(token);
    }

    public StringProperty getTokenRow() {
        return tokenRow;
    }

    public void setTokenRow(String token) {
        this.tokenRow.set(token);
    }
}