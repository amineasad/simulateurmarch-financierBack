package tn.esprit.examen.nomPrenomClasseExamen.services.userbook;

import tn.esprit.examen.nomPrenomClasseExamen.services.userbook.UserOrderIndex.ActiveOrder;

import java.time.Instant;
import java.util.List;

public record UserBookSnapshot(
        Long userId,
        Long assetId,
        int totalBuyQty,
        int totalSellQty,
        List<ActiveOrder> activeOrders,
        Instant snapshotTs
) {}

