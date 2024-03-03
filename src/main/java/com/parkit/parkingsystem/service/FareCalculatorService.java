package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket, boolean discount){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        double inHour = ticket.getInTime().getTime();
        double outHour = ticket.getOutTime().getTime();

        double duration = (outHour - inHour) / (1000 * 60 * 60);

        if(duration < 0.5){
            duration = 0;
        }

        double priceMultiplier = discount ? 0.95 : 1;

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                ticket.setPrice(Math.round(duration * Fare.CAR_RATE_PER_HOUR * priceMultiplier * 100.0) / 100.0);
                break;
            }
            case BIKE: {
                ticket.setPrice(Math.round(duration * Fare.BIKE_RATE_PER_HOUR * priceMultiplier * 100.0) / 100.0);
                break;
            }
            default: throw new IllegalArgumentException("Unknown Parking Type");
        }
    }
}