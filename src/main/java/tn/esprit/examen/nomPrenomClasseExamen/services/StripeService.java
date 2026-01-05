package tn.esprit.examen.nomPrenomClasseExamen.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StripeService {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    public Map<String, String> createCheckoutSession(Long userId, Double amount) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        // Convertir le montant en centimes (Stripe utilise les centimes)
        long amountInCents = (long) (amount * 100);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("http://localhost:4200/wallet/success?session_id={CHECKOUT_SESSION_ID}&userId=" + userId + "&amount=" + amount)
                .setCancelUrl("http://localhost:4200/wallet/cancel")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Dépôt sur compte TRADIX")
                                                                .setDescription("Alimentation du portefeuille de trading")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        Session session = Session.create(params);

        Map<String, String> response = new HashMap<>();
        response.put("sessionId", session.getId());
        response.put("url", session.getUrl());

        return response;
    }
}