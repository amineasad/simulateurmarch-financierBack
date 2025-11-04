package tn.esprit.examen.nomPrenomClasseExamen.services.userbook;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tn.esprit.examen.nomPrenomClasseExamen.entities.Order;
import tn.esprit.examen.nomPrenomClasseExamen.entities.OrderSide;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserBookService {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<UserAssetKey, UserOrderIndex> indexByUserAsset = new ConcurrentHashMap<>();

    public void onOrderAddedOrUpdated(Order order) {
        UserAssetKey key = new UserAssetKey(order.getUserId(), order.getAsset().getId());
        UserOrderIndex idx = indexByUserAsset.computeIfAbsent(key, k -> new UserOrderIndex());

        // Recalcule simple: on reconstruit la liste pour ce user/asset à partir de l'ordre courant
        // (Dans une version avancée, on maintiendrait un set; ici, on assure la cohérence minimale)
        List<UserOrderIndex.ActiveOrder> list = new ArrayList<>();
        UserOrderIndex.ActiveOrder ao = new UserOrderIndex.ActiveOrder();
        ao.orderId = order.getId();
        ao.price = order.getPrice();
        ao.remaining = order.getRemainingQuantity();
        ao.side = order.getSide();
        ao.createdAt = order.getCreatedAt();
        list.add(ao);

        int buy = (order.getSide() == OrderSide.BUY) ? order.getRemainingQuantity() : 0;
        int sell = (order.getSide() == OrderSide.SELL) ? order.getRemainingQuantity() : 0;

        idx.getActiveOrders().clear();
        idx.getActiveOrders().addAll(list);
        idx.setTotals(buy, sell);
        idx.setSnapshotTs(Instant.now());

        publishAfterCommit(order.getUserId(), order.getAsset().getId(), idx);
    }

    public void onOrderRemoved(Order order) {
        UserAssetKey key = new UserAssetKey(order.getUserId(), order.getAsset().getId());
        indexByUserAsset.remove(key);
        publishAfterCommit(order.getUserId(), order.getAsset().getId(), null);
    }

    public UserBookSnapshot getSnapshot(Long userId, Long assetId) {
        UserAssetKey key = new UserAssetKey(userId, assetId);
        UserOrderIndex idx = indexByUserAsset.get(key);
        if (idx == null) {
            return new UserBookSnapshot(userId, assetId, 0, 0, List.of(), Instant.now());
        }
        return new UserBookSnapshot(userId, assetId, idx.getTotalBuyQty(), idx.getTotalSellQty(), idx.getActiveOrders(), idx.getSnapshotTs());
    }

    private void publishAfterCommit(Long userId, Long assetId, UserOrderIndex idx) {
        Runnable task = () -> {
            UserBookSnapshot snapshot = getSnapshot(userId, assetId);
            messagingTemplate.convertAndSend("/topic/userbook/" + userId + "/" + assetId, snapshot);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { task.run(); }
            });
        } else {
            task.run();
        }
    }
}

