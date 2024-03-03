package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private final static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp(){
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        TicketDAO.setDataBaseConfig(dataBaseTestConfig);
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown(){

    }

    @Test
    void testParkingACar(){
        Clock mockClock = mock(Clock.class);
        when(mockClock.instant()).thenReturn(Instant.parse("2000-01-01T00:00:00.000Z"));
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, mockClock);

        // Enter parking
        parkingService.processIncomingVehicle();

        // Check that a ticket is actually saved in DB and Parking table is updated with availability
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket);
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        assertFalse(parkingSpot.isAvailable());
    }

    @Test
    void testParkingLotExit(){
        // Mock clock for enter and exit time
        Clock mockClock = mock(Clock.class);
        Instant first = Instant.parse("2000-01-01T00:00:00.000Z");
        Instant second = first.plus(1, ChronoUnit.HOURS);
        when(mockClock.instant()).thenReturn(first, second);

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, mockClock);

        // Enter and exit parking
        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();

        // Check that the ticket price is good
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertEquals(Fare.CAR_RATE_PER_HOUR, ticket.getPrice());
    }

    @Test
    void testParkingLotExitRecurringUser(){
        // Mock clock time for next four times
        Clock mockClock = mock(Clock.class);
        // first ticket
        Instant first = Instant.parse("2000-01-01T00:00:00.000Z");
        Instant second = first.plus(1, ChronoUnit.HOURS);
        // second ticket
        Instant third = second.plus(1, ChronoUnit.HOURS);
        Instant fourth = third.plus(1, ChronoUnit.HOURS);
        when(mockClock.instant()).thenReturn(first, second, third, fourth);

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, mockClock);

        // Enter parking once
        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();

        // Enter parking a second time
        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();

        // Check that we have two tickets
        assertEquals(2, ticketDAO.getNbTicket("ABCDEF"));

        // Check that the second ticket have a 5% discount
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertEquals(Math.round(Fare.CAR_RATE_PER_HOUR * 0.95 * 100.0) / 100.0, ticket.getPrice());
    }
}