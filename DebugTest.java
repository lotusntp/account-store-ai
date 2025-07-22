// Debug test to understand the issue
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DebugTest {
    public static void main(String[] args) {
        // Create instances similar to the test
        Category category = new Category("Games");
        Product product = new Product();
        
        Stock stock1 = new Stock();
        stock1.setSold(false);
        stock1.setReservedUntil(null);
        
        Stock stock2 = new Stock();
        stock2.setSold(true);
        stock2.setSoldAt(LocalDateTime.now());
        
        Stock stock3 = new Stock();
        stock3.setSold(false);
        stock3.setReservedUntil(LocalDateTime.now().plusMinutes(30));
        
        System.out.println("Before adding stock items:");
        System.out.println("Total stock count: " + product.getTotalStockCount());
        System.out.println("Stock items size: " + product.getStockItems().size());
        
        // Add stock items
        product.addStockItem(stock1);
        System.out.println("After adding stock1:");
        System.out.println("Total stock count: " + product.getTotalStockCount());
        System.out.println("Stock items size: " + product.getStockItems().size());
        
        product.addStockItem(stock2);
        System.out.println("After adding stock2:");
        System.out.println("Total stock count: " + product.getTotalStockCount());
        System.out.println("Stock items size: " + product.getStockItems().size());
        
        product.addStockItem(stock3);
        System.out.println("After adding stock3:");
        System.out.println("Total stock count: " + product.getTotalStockCount());
        System.out.println("Stock items size: " + product.getStockItems().size());
        
        System.out.println("Available stock count: " + product.getAvailableStockCount());
        System.out.println("Sold stock count: " + product.getSoldStockCount());
        System.out.println("Reserved stock count: " + product.getReservedStockCount());
    }
}