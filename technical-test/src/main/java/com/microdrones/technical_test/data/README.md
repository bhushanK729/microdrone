#configurations

- [verticalSpeeds][ascension] the vertical speed of the drone during an ascension in meter / seconds (m/s)
- [verticalSpeeds][descent] the vertical speed of the drone during a descent in meter / seconds (m/s)
- [energy][numberOfBatteries] is the number of batteries mounted on the drone
- [energy][capacity] is the capacity of the battery in milli Ampere Hour (mAh)
- [payload][additionalLoad] is the extra load in in Ampere x Seconds ² / Meters ² (A.s²/m² ) caused by the payload (see [currentLoadInFlight])

#drones

[currentLoadInFlight] in Ampere x Seconds ² / Meters ² (A.s²/m² )
  The current load in A per square unit of speed ((m/s)²) on the drone power system when it is flying
   - In vertical ascension [ascension]
   - In vertical descent  [descent]
   - In horizontal translation  [translation]

    NOTE: We assume that this to remain constant thorough the mission.

[forcedLandingCharge] in milli Ampere Hour (mAh)
   - When the available charge is the batteries is less or equal to [forcedLandingCharge], the drone will land

#missions

- [horizontalSpeed] the horizontal speed of the drone during the flight in meter / seconds (m/s)
- [altitude] the flying altitude in meters (m)
- [points] is a ordered array of geographical coordinates representing the flight path
