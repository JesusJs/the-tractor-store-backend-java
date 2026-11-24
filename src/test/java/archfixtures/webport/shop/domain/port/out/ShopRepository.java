package archfixtures.webport.shop.domain.port.out;

/** Outbound port that a controller must not use directly. */
public interface ShopRepository {
  String find();
}
