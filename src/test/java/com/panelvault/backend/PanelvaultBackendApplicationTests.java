package com.panelvault.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba de humo: la aplicacion completa arranca y queda conectada a la base de pruebas.
 *
 * <p>Verificar el nombre de la base protege los datos de desarrollo: si alguien quitara el
 * perfil "test", esta prueba fallaria antes de que otras pruebas escribieran en panelvault_dev.
 */
@SpringBootTest
@ActiveProfiles("test")
class PanelvaultBackendApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void arrancaConectadoALaBaseDePruebas() {
		String database = jdbcTemplate.queryForObject("SELECT current_database()", String.class);

		assertThat(database).isEqualTo("panelvault_test");
	}
}