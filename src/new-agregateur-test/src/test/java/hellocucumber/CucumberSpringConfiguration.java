package hellocucumber;

import com.dimsoft.agregateur.new_agregateur_prod.NewAgregateurProdApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@CucumberContextConfiguration
@SpringBootTest(classes = NewAgregateurProdApplication.class) // Remplace par ta classe main Spring Boot
public class CucumberSpringConfiguration {
}
