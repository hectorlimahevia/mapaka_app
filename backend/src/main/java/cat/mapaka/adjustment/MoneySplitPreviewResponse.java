package cat.mapaka.adjustment;

import cat.mapaka.money.WalletType;

import java.math.BigDecimal;
import java.util.List;

/** Com es repartiria una bonificació amb el repartiment habitual del fill. goalName només
 * s'omple a les parts de tipus GOAL. */
public record MoneySplitPreviewResponse(List<Part> parts) {

    public record Part(WalletType wallet, String goalName, BigDecimal amount) {
    }
}
