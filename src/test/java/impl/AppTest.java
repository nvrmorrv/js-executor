package impl;

import impl.controllers.interceptors.RequestCountInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
@AutoConfigureMockMvc
@MockBean(RequestCountInterceptor.class)
class AppTest {
    @Test
    void shouldPassOnRisingOfAppContext() { }
}
