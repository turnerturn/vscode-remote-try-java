package com.mycompany.app;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CpamDriverDownloader {

    private String myTerminalAccessLevelName = "WestTulsa_Rack";
    private DriverRepository driverRepository = new DriverRepository();
    private CarrierDriverRepository carrierDriverRepository = new CarrierDriverRepository();

    /**
     * Processes a list of CpamPerson objects.
     * @param persons List of CpamPerson objects to process.
     */
    public void process(List<CpamPerson> persons) {
        for (CpamPerson person : persons) {
            process(person);
        }
    }

    /**
     * Processes a single CpamPerson object.
     * @param person CpamPerson object to process.
     */
    protected void process(CpamPerson person) {
        List<CpamPerson.Badge> badges = collectBadgesWithMyAccessLevel(person);
        Driver driver = merge(driverRepository.findDriverByPersonId(person.getPersonId()).orElse(initNewDriver(person)), person);

        if (driver.getId() != null && (badges == null || badges.isEmpty())) {
            carrierDriverRepository.deleteAll(driver.getCarrierDrivers());
            publishDeleteDriverEvent(driver);
            return;
        }

        if (driver.getId() == null && (badges == null || badges.isEmpty())) {
            return;
        }

        if (driver.getId() == null) {
            driver.setId(getNextDriverId());
        }

        Set<CarrierDriver> carrierDriversSet = new HashSet<>();
        Set<CarrierDriver> reassignedCarrierDrivers = new HashSet<>();
        for (CpamPerson.Badge badge : badges) {
            CarrierDriver temporaryCarrierDriver = initTemporaryCarrierDriver(badge);
            carrierDriversSet.add(temporaryCarrierDriver);

            for (CarrierDriver carrierDriver : carrierDriverRepository.findAllByBadgeNumber(badge.getNumber())) {
                driverRepository.findByDriverId(carrierDriver.getDriverId()).ifPresent(d -> {
                    // If they don't match, add to the reassignedCarrierDrivers
                    if (!match(d.getPersonId(), person.getUnid())) {
                        // Cascade and eager to push to driver.
                        carrierDriverRepository.delete(carrierDriver);
                        // To be used when publishing updated driver events.
                        reassignedCarrierDrivers.add(carrierDriver);
                    } else {
                        // Reassigned carrier-drivers will not be added to our driver because we don't want to preserve the carrier-driver specs when they were intended for a different driver.
                        carrierDriversSet.add(carrierDriver);
                    }
                });
            }
        }

        driver.setCarrierDrivers(new ArrayList<>());
        for (CarrierDriver carrierDriver : carrierDriversSet) {
            carrierDriver.setDriverId(driver.getId());
            badges.stream().filter(b -> match(b.getNumber(), carrierDriver.getBadgeNumber())).findFirst().ifPresent(b -> {
                CarrierDriver cd = merge(carrierDriver, b);
                driver.getCarrierDrivers().add(cd);
            });
        }
    }

    /**
     * Collects badges without the terminal access level.
     * @param person CpamPerson object.
     * @return List of badges without the terminal access level.
     */
    protected List<CpamPerson.Badge> collectBadgesWithoutMyAccessLevel(CpamPerson person) {
        // Implementation to collect badges without my terminal access level
        return person.getBadges();
    }

    /**
     * Collects badges with the terminal access level.
     * @param person CpamPerson object.
     * @return List of badges with the terminal access level.
     */
    protected List<CpamPerson.Badge> collectBadgesWithMyAccessLevel(CpamPerson person) {
        // Implementation to collect badges with my terminal access level
        return person.getBadges();
    }

    /**
     * Merges badge details into a carrier-driver.
     * @param carrierDriver CarrierDriver object.
     * @param badge Badge object.
     * @return Merged CarrierDriver object.
     */
    protected CarrierDriver merge(final CarrierDriver carrierDriver, final CpamPerson.Badge badge) {
        // Implementation to merge badge details into carrier-driver
        carrierDriver.setBadgeNumber(badge.getNumber());
        carrierDriver.setBadgeStatus(badge.getStatus());
        return carrierDriver;
    }

    /**
     * Merges person details into a driver.
     * @param driver Driver object.
     * @param person CpamPerson object.
     * @return Merged Driver object.
     */
    protected Driver merge(final Driver driver, final CpamPerson person) {
        // Implementation to merge person details into driver
        driver.setFirstName(person.getFirstName());
        driver.setLastName(person.getLastName());
        driver.setStatus(person.getStatus());
        driver.setPersonId(person.getPersonId());
        driver.setCpamPersonUnid(person.getUnid());
        driver.setEffectiveDate(person.getEffectiveDate());
        driver.setExpirationDate(person.getExpirationDate());
        return driver;
    }

    /**
     * Initializes a new driver.
     * @param person CpamPerson object.
     * @return Initialized Driver object.
     */
    protected Driver initNewDriver(CpamPerson person) {
        // Implementation to initialize a new driver
        Driver driver = new Driver();
        driver.setFirstName(person.getFirstName());
        driver.setLastName(person.getLastName());
        driver.setStatus(person.getStatus());
        driver.setPersonId(person.getPersonId());
        driver.setCpamPersonUnid(person.getUnid());
        driver.setEffectiveDate(person.getEffectiveDate());
        driver.setExpirationDate(person.getExpirationDate());
        return driver;
    }

    /**
     * Gets the next driver ID.
     * @return Next driver ID.
     */
    protected String getNextDriverId() {
        // Implementation to get the next driver ID
        return "newDriverId";
    }

    /**
     * Initializes a temporary carrier driver.
     * @param badge Badge object.
     * @return Initialized CarrierDriver object.
     */
    protected CarrierDriver initTemporaryCarrierDriver(CpamPerson.Badge badge) {
        // Implementation to initialize a temporary carrier driver
        CarrierDriver carrierDriver = new CarrierDriver();
        carrierDriver.setBadgeNumber(badge.getNumber());
        carrierDriver.setBadgeStatus(badge.getStatus());
        return carrierDriver;
    }

    /**
     * Matches two string values.
     * @param value1 First string value.
     * @param value2 Second string value.
     * @return True if values match, false otherwise.
     */
    protected boolean match(String value1, String value2) {
        String sanitizedValue1 = Optional.ofNullable(value1).map(String::trim).orElse("");
        String sanitizedValue2 = Optional.ofNullable(value2).map(String::trim).orElse("");
        return sanitizedValue1.equalsIgnoreCase(sanitizedValue2);
    }

    /**
     * Matches two integer values.
     * @param value1 First integer value.
     * @param value2 Second integer value.
     * @return True if values match, false otherwise.
     */
    protected boolean match(Integer value1, Integer value2) {
        return match(String.valueOf(value1), String.valueOf(value2));
    }

    /**
     * Publishes a delete driver event.
     * @param driver Driver object.
     */
    protected void publishDeleteDriverEvent(Driver driver) {
        // Implementation to publish delete driver event
        System.out.println("Driver deleted: " + driver);
    }

    /**
     * Publishes an update driver event.
     * @param driver Driver object.
     */
    protected void publishUpdateDriverEvent(Driver driver) {
        // Implementation to publish update driver event
        System.out.println("Driver updated: " + driver);
    }
}

@Data
class Driver {
    private String id;
    private String firstName;
    private String lastName;
    private String status;
    private String personId;
    private String cpamPersonUnid;
    private Long effectiveDate;
    private Long expirationDate;
    private List<CarrierDriver> carrierDrivers;
}

@Data
class CarrierDriver {
    private String carrierId;
    private String driverId;
    private String badgeNumber;
    private String badgeStatus;
}

@Data
class CpamPerson {
    private String unid;
    private String firstName;
    private String lastName;
    private String status;
    private String personId;
    private Long effectiveDate;
    private Long expirationDate;
    private List<Badge> badges;

    @Data
    public static class Badge {
        private String number;
        private String status;
        private Long effectiveDate;
        private Long expirationDate;
        private List<AccessLevel> accessLevels;
    }

    @Data
    public static class AccessLevel {
        private String unid;
        private String name;
    }
}

class DriverRepository {
    /**
     * Finds a driver by person ID.
     * @param personId Person ID.
     * @return Optional containing the found Driver, or empty if not found.
     */
    public Optional<Driver> findDriverByPersonId(String personId) {
        // Implementation to find driver by person ID
        return Optional.empty();
    }

    /**
     * Finds a driver by driver ID.
     * @param driverId Driver ID.
     * @return Optional containing the found Driver, or empty if not found.
     */
    public Optional<Driver> findByDriverId(String driverId) {
        // Implementation to find driver by driver ID
        return Optional.empty();
    }
}

class CarrierDriverRepository {
    /**
     * Finds all carrier drivers by badge number.
     * @param badgeNumber Badge number.
     * @return List of CarrierDriver objects.
     */
    public List<CarrierDriver> findAllByBadgeNumber(String badgeNumber) {
        // Implementation to find all carrier drivers by badge number
        return new ArrayList<>();
    }

    /**
     * Deletes all carrier drivers.
     * @param carrierDrivers List of CarrierDriver objects to delete.
     */
    public void deleteAll(List<CarrierDriver> carrierDrivers) {
        // Implementation to delete all carrier drivers
    }

    /**
     * Deletes a carrier driver.
     * @param carrierDriver CarrierDriver object to delete.
     */
    public void delete(CarrierDriver carrierDriver) {
        // Implementation to delete a carrier driver
    }
}