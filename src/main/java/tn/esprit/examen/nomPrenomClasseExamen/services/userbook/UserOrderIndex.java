package tn.esprit.examen.nomPrenomClasseExamen.services.userbook;

import tn.esprit.examen.nomPrenomClasseExamen.entities.OrderSide;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UserOrderIndex {
    public static class ActiveOrder {
        public Long orderId;
        public BigDecimal price;
        public int remaining;
        public OrderSide side;
        public Instant createdAt;
    }

    private int totalBuyQty;
    private int totalSellQty;
    private final List<ActiveOrder> activeOrders = new ArrayList<>();
    private Instant snapshotTs;

    public int getTotalBuyQty() { return totalBuyQty; }
    public int getTotalSellQty() { return totalSellQty; }
    public List<ActiveOrder> getActiveOrders() { return activeOrders; }
    public Instant getSnapshotTs() { return snapshotTs; }

    public void setTotals(int buy, int sell) {
        this.totalBuyQty = buy;
        this.totalSellQty = sell;
    }

    public void setSnapshotTs(Instant ts) { this.snapshotTs = ts; }
}

