package com.refind.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LostItemAppStartupSafetyTest {

    @AfterEach
    void clearDemoSeedProperty() {
        System.clearProperty("refind.seedDemoData");
    }

    @Test
    void normalStartupDoesNotRunTheDemoSeeder() {
        System.setProperty("refind.seedDemoData", "false");
        List<String> existingRecords = new ArrayList<>(List.of("existing-user", "existing-item"));

        assertFalse(LostItemApp.runDemoSeedIfEnabled(() -> existingRecords.add("demo-record")));
        assertEquals(List.of("existing-user", "existing-item"), existingRecords,
                "Normal startup must preserve existing records by not seeding demo data.");
    }

    @Test
    void demoSeedingRequiresExplicitOptIn() {
        System.setProperty("refind.seedDemoData", "true");
        AtomicInteger seedCalls = new AtomicInteger();

        assertTrue(LostItemApp.runDemoSeedIfEnabled(seedCalls::incrementAndGet));
        assertEquals(1, seedCalls.get());
    }

    @Test
    void normalStartupDoesNotCreateAnAdministratorAutomatically() {
        System.clearProperty("refind.seedDemoData");
        System.clearProperty("refind.autoInitializeSchema");

        assertFalse(LostItemApp.isDemoDataSeedingEnabled());
        assertFalse(Boolean.getBoolean("refind.autoInitializeSchema"));
    }
}
