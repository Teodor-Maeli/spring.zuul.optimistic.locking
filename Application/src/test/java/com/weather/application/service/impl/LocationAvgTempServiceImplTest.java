package com.weather.application.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.weather.application.domain.LocationAvgTemp;
import com.weather.application.repository.LocationAvgTempRepository;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;


@SpringBootTest
class LocationAvgTempServiceImplTest {

  @Autowired
  private LocationAvgTempRepository locationAvgTempRepository;

  @Autowired
  private WebApplicationContext context;
  private MockMvc mvc;

  @BeforeEach
  public void setup(){
    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .build();
  }

  @AfterEach
  public void cleanUp(){
    locationAvgTempRepository.deleteAllByLocation("VAR");
  }

  private final String URI = "/VAR/1";
  private final int THREAD_POOL_SIZE = 5;


  @Test
  public void withoutConcurrencyTransactions() throws Exception {
    //given
    final LocationAvgTemp srcTemp = locationAvgTempRepository.save(new LocationAvgTemp("VAR", 1.0));
    assertEquals(1, srcTemp.getCounter());
    assertEquals(1.0, srcTemp.getSum());

    //when
    mvc.perform(post(URI));

    final LocationAvgTemp modifiedTemp = locationAvgTempRepository.findByLocation(
        srcTemp.getLocation());

    //then
    assertAll(
        () -> assertEquals(2, modifiedTemp.getCounter()),
        () -> assertEquals(2, modifiedTemp.getSum()));

  }

  @Test
  public void testWithConcurrencyTransactions() throws InterruptedException {
    //given
    final LocationAvgTemp srcTemp = locationAvgTempRepository.save(new LocationAvgTemp("VAR", 1.0));
    assertEquals(1, srcTemp.getCounter());
    assertEquals(1.0, srcTemp.getSum());

    final ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    //when
    for (int i = 0 ; i < THREAD_POOL_SIZE; i++) {
      executor.execute(() -> {
        try {
          mvc.perform(post(URI));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
    }
    executor.shutdown();
    executor.awaitTermination(3, TimeUnit.MINUTES);

    //then
    final LocationAvgTemp modifiedTemp = locationAvgTempRepository.findByLocation(
        srcTemp.getLocation());

    assertAll(
        () -> assertEquals(6, modifiedTemp.getCounter()),
        () -> assertEquals(6, modifiedTemp.getSum()));
  }
}