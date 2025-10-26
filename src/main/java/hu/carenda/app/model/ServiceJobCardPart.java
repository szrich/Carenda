package hu.carenda.app.model;

import javafx.beans.property.*;

public class ServiceJobCardPart {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty sjc_id = new SimpleIntegerProperty();
    private final StringProperty sku = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final DoubleProperty quantity = new SimpleDoubleProperty();
    private final IntegerProperty unit_price_cents = new SimpleIntegerProperty();
    private final IntegerProperty vat_percent = new SimpleIntegerProperty();
    private final IntegerProperty sort_order = new SimpleIntegerProperty();

    public ServiceJobCardPart() {
    }

    public ServiceJobCardPart(int id,
                              int sjc_id,
                              String sku,
                              String name,
                              double quantity,
                              int unit_price_cents,
                              int vat_percent,
                              int sort_order) {
        setId(id);
        setSjc_id(sjc_id);
        setSku(sku);
        setName(name);
        setQuantity(quantity);
        setUnit_price_cents(unit_price_cents);
        setVat_percent(vat_percent);
        setSort_order(sort_order);
    }

    // id
    public int getId() { return id.get(); }
    public void setId(int v) { id.set(v); }
    public IntegerProperty idProperty() { return id; }

    // sjc_id
    public int getSjc_id() { return sjc_id.get(); }
    public void setSjc_id(int v) { sjc_id.set(v); }
    public IntegerProperty sjc_idProperty() { return sjc_id; }

    // sku (cikkszám, lehet null)
    public String getSku() { return sku.get(); }
    public void setSku(String v) { sku.set(v); }
    public StringProperty skuProperty() { return sku; }

    // name (megnevezés)
    public String getName() { return name.get(); }
    public void setName(String v) { name.set(v); }
    public StringProperty nameProperty() { return name; }

    // quantity (mennyiség, lehet tört pl. 2.5)
    public double getQuantity() { return quantity.get(); }
    public void setQuantity(double v) { quantity.set(v); }
    public DoubleProperty quantityProperty() { return quantity; }

    // unit_price_cents (egységár fillérben)
    public int getUnit_price_cents() { return unit_price_cents.get(); }
    public void setUnit_price_cents(int v) { unit_price_cents.set(v); }
    public IntegerProperty unit_price_centsProperty() { return unit_price_cents; }

    // vat_percent (ÁFA %)
    public int getVat_percent() { return vat_percent.get(); }
    public void setVat_percent(int v) { vat_percent.set(v); }
    public IntegerProperty vat_percentProperty() { return vat_percent; }

    // sort_order
    public int getSort_order() { return sort_order.get(); }
    public void setSort_order(int v) { sort_order.set(v); }
    public IntegerProperty sort_orderProperty() { return sort_order; }
}
