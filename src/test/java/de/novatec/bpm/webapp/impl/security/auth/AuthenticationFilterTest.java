package de.novatec.bpm.webapp.impl.security.auth;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AuthenticationFilterTest {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
      {"http://localhost:8080/camunda/app/tasklist/default/", "default"},
      {"http://localhost:8080/camunda/app/cockpit/default/", "default"},
      {"http://localhost:8080/camunda/app/admin/default/", "default"},
      {"http://localhost:8080/camunda/app/welcome/default/", "default"},
      {"http://localhost:8080/camunda/app/welcome/", "default"},
      {"http://localhost:8080/camunda/app/", "default"},
      {"http://localhost:8080/camunda/", "default"},
      {"http://localhost:8080/camunda/app/cockpit/styles/styles.css", "default"},
      {"http://localhost:8080/camunda/api/engine/engine/", "default"},
      {"http://localhost:8080/camunda/api/admin/auth/user/default", "default"},
      {"http://localhost:8080/camunda/app/cockpit/tenant23/", "tenant23"}
    });
  }

  @Parameter
  public String url;
  
  @Parameter(1)
  public String engineName;

  @Test
  public void testUrlParsing() {
    AuthenticationFilter filter = new AuthenticationFilter();
    String[] appInfo = filter.getAppInfo(url);
    assertEquals(engineName, filter.getEngineName(appInfo));
  }

}
