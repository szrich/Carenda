package hu.carenda.app.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ServiceJobCardWorkDesc {

    private final ObjectProperty<Integer> id = new SimpleObjectProperty<>(null);
    private final IntegerProperty sjc_id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final DoubleProperty hours = new SimpleDoubleProperty();
    private final IntegerProperty rate_cents = new SimpleIntegerProperty();
    private final IntegerProperty vat_percent = new SimpleIntegerProperty();
    private final IntegerProperty sort_order = new SimpleIntegerProperty();

    public ServiceJobCardWorkDesc() {
    }

    public ServiceJobCardWorkDesc(Integer id,
                                  int sjc_id,
                                  String name,
                                  double hours,
                                  int rate_cents,
                                  int vat_percent,
                                  int sort_order) {
        setId(id);
        setSjc_id(sjc_id);
        setName(name);
        setHours(hours);
        setRate_cents(rate_cents);
        setVat_percent(vat_percent);
        setSort_order(sort_order);
    }

    // --- id ---
    public Integer getId() { return id.get(); }
    public void setId(Integer v) { id.set(v); }
    public ObjectProperty<Integer> idProperty() { return id; }

    // --- sjc_id (hivatkozás servicejobcard.id-re) ---
    public int getSjc_id() { return sjc_id.get(); }
    public void setSjc_id(int v) { sjc_id.set(v); }
    public IntegerProperty sjc_idProperty() { return sjc_id; }

    // name (pl. "Fékbetét csere")
    public String getName() { return name.get(); }
    public void setName(String v) { name.set(v); }
    public StringProperty nameProperty() { return name; }

    // hours (munkaóra, lehet tört szám pl. 1.5)
    public double getHours() { return hours.get(); }
    public void setHours(double v) { hours.set(v); }
    public DoubleProperty hoursProperty() { return hours; }

    // rate_cents (óradíj fillérben)
    public int getRate_cents() { return rate_cents.get(); }
    public void setRate_cents(int v) { rate_cents.set(v); }
    public IntegerProperty rate_centsProperty() { return rate_cents; }

    // vat_percent (ÁFA %, pl. 27)
    public int getVat_percent() { return vat_percent.get(); }
    public void setVat_percent(int v) { vat_percent.set(v); }
    public IntegerProperty vat_percentProperty() { return vat_percent; }

    // sort_order (sorrend a munkalapon)
    public int getSort_order() { return sort_order.get(); }
    public void setSort_order(int v) { sort_order.set(v); }
    public IntegerProperty sort_orderProperty() { return sort_order; }
}
