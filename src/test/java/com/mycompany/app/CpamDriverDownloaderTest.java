package com.mycompany.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class CpamDriverDownloaderTest {

    private List<CpamPerson> sampleData;

    @BeforeEach
    public void setUp() throws IOException {
        // Load sample data from JSON file
        ObjectMapper objectMapper = new ObjectMapper();
        sampleData = objectMapper.readValue(
                Files.readAllBytes(Paths.get("src/test/resources/cpam-persons-sample.json")),
                new TypeReference<List<CpamPerson>>() {}
        );
    }

    @Test
    public void testProcessWithEmptyBadges() {
        CpamDriverDownloader downloader = new CpamDriverDownloader();
        DriverRepository driverRepository = mock(DriverRepository.class);
        CarrierDriverRepository carrierDriverRepository = mock(CarrierDriverRepository.class);
        
        // Arrange
        CpamPerson person = sampleData.get(0); // Assuming the first person has empty badges
        person.setBadges(new ArrayList<>());
        
        when(driverRepository.findDriverByPersonId(anyString())).thenReturn(Optional.empty());
        
        // Act
        downloader.process(Arrays.asList(person));
        
        // Assert
        verify(carrierDriverRepository, never()).deleteAll(anyList());
    }

    @Test
    public void testProcessWithExistingDriverAndBadges() {
        CpamDriverDownloader downloader = new CpamDriverDownloader();
        DriverRepository driverRepository = mock(DriverRepository.class);
        CarrierDriverRepository carrierDriverRepository = mock(CarrierDriverRepository.class);
        
        // Arrange
        CpamPerson person = sampleData.get(1); // Assuming the second person has badges
        CpamPerson.Badge badge = new CpamPerson.Badge();
        
        badge.setNumber("12345");
        person.setBadges(Arrays.asList(badge));
        
        Driver driver = new Driver();
        driver.setId("1");
        
        when(driverRepository.findDriverByPersonId(anyString())).thenReturn(Optional.of(driver));
        
        // Act
        downloader.process(Arrays.asList(person));
        
        // Assert
        
    }
}