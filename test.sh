#!/bin/bash
#http://developer.onebusaway.org/modules/onebusaway-application-modules/current/api/where/methods/trip-details.html

DATE=`date +"%s000"`
#STOP="MTA_404190"
#STOP="MTA_305438"
STOP="MTA_202899"
#TRIP=MTA NYCT_YU_W3-Weekday-071000_MISC_153
#TRIP=MTA%20NYCT_YU_D3-Weekday-SDon-045000_MISC_103
#TRIP="MTA%20NYCT_YU_W3-Weekday-071000_MISC_153"
#TRIP="MTA%20NYCT_YU_D3-Sunday-054000_MISC_135"
TANDS=(`wget -q -O - http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/arrivals-and-departures-for-stop/${STOP}.xml?key=TEST | grep -e tripId -e scheduledArrivalTime | head -n 2 | grep -o '>.*<' | sed -e 's!<!!' | sed -e 's!>!!' | sed -e 's! !%20!g'`)
TRIP=${TANDS[0]}
ATIME=${TANDS[1]}
SHAPE="MTA_X10341"
ROUTE="MTA%20NYCT_S61"
#VEHICLE="MTA%20NYCT_2420"
VEHICLE="MTA%20NYCT_4008"

echo "currentTime=${DATE} and tripId=${TRIP} at ${ATIME}"

echo "TEST:  agencies with coverage"
./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/agencies-with-coverage.xml?key=TEST http://localhost:8080/onebusaway-api-webapp/api/where/agencies-with-coverage.xml?key=TEST

echo "TEST:  agencies"
./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/agency/MTABC.xml?key=TEST http://localhost:8080/onebusaway-api-webapp/api/where/agency/MTABC.xml?key=TEST

echo "TEST:  arrivals and departures for stop"
API=arrivals-and-departures-for-stop
./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/${API}/${STOP}.xml?key=TEST http://localhost:8080/onebusaway-api-webapp/api/where/${API}/${STOP}.xml?key=TEST

echo "TEST:  arrival and departure for stop"
API=arrival-and-departure-for-stop
./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/${API}/${STOP}.xml?key=TEST\&serviceDate=${ATIME}\&tripId=${TRIP}\&time=${ATIME} http://localhost:8080/onebusaway-api-webapp/api/where/${API}/${STOP}.xml?key=TEST\&serviceDate=${ATIME}\&tripId=${TRIP}\&time=${ATIME}


echo "TEST:  route-ids-for-agency"
./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/route-ids-for-agency/MTABC.xml?key=TEST http://localhost:8080/onebusaway-api-webapp/api/where/route-ids-for-agency/MTABC.xml?key=TEST

echo "TEST:  route"
./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/route/MTABC_Q50.xml?key=TEST http://localhost:8080/onebusaway-api-webapp/api/where/route/MTABC_Q50.xml?key=TEST

echo "TEST:  routes-for-agency"
./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/routes-for-agency/MTABC.xml?key=TEST http://localhost:8080/onebusaway-api-webapp/api/where/routes-for-agency/MTABC.xml?key=TEST

echo "TEST:  routes-for-location"
ARGS='lat=40.704847&lon=-74.014260'
./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/routes-for-location.xml?key=TEST\&${ARGS} http://localhost:8080/onebusaway-api-webapp/api/where/routes-for-location.xml?key=TEST\&${ARGS}

echo "TEST:  schedule for stop"
API=schedule-for-stop
./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/${API}/${STOP}.xml?key=TEST http://localhost:8080/onebusaway-api-webapp/api/where/${API}/${STOP}.xml?key=TEST 

echo "TEST:  shape"
API=shape
./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/${API}/${SHAPE}.xml?key=TEST http://localhost:8080/onebusaway-api-webapp/api/where/${API}/${SHAPE}.xml?key=TEST

echo "TEST:  stop-ids-for-agency"
./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/routes-for-agency/MTABC.xml?key=TEST http://localhost:8080/onebusaway-api-webapp/api/where/routes-for-agency/MTABC.xml?key=TEST

echo "TEST:  stop"
API=stop
./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/${API}/${STOP}.xml?key=TEST http://localhost:8080/onebusaway-api-webapp/api/where/${API}/${STOP}.xml?key=TEST

echo "TEST:  stops-for-location"
ARGS='lat=40.704847&lon=-74.014260'
./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/routes-for-location.xml?key=TEST\&${ARGS} http://localhost:8080/onebusaway-api-webapp/api/where/routes-for-location.xml?key=TEST\&${ARGS}

echo "TEST:  stops for route"
API=stops-for-route
./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/${API}/${ROUTE}.xml?key=TEST\&version=2 http://localhost:8080/onebusaway-api-webapp/api/where/${API}/${ROUTE}.xml?key=TEST\&version=2

echo "TEST:  trip details"
API="trip-details"
./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/${API}/${TRIP}.xml?key=TEST http://localhost:8080/onebusaway-api-webapp/api/where/${API}/${TRIP}.xml?key=TEST

echo "TEST:  trip for vehicle"
API="trip-for-vehicle"
./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/${API}/${VEHICLE}.xml?key=TEST http://localhost:8080/onebusaway-api-webapp/api/where/${API}/${VEHICLE}.xml?key=TEST -v
echo "TEST:  FAIL"
exit 1;

echo "TEST:  trip"
API="trip"

./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/${API}/${TRIP}.xml?key=TEST http://localhost:8080/onebusaway-api-webapp/api/where/${API}/${TRIP}.xml?key=TEST

echo "TEST:  trips-for-location"
ARGS='lat=40.704847&lon=-74.014260'
./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/trips-for-location.xml?key=TEST\&${ARGS} http://localhost:8080/onebusaway-api-webapp/api/where/trips-for-location.xml?key=TEST\&${ARGS}

echo "TEST:  trips for route"
API=trips-for-route
./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/${API}/${ROUTE}.xml?key=TEST\&version=2 http://localhost:8080/onebusaway-api-webapp/api/where/${API}/${ROUTE}.xml?key=TEST\&version=2

echo "TEST:  vehicle-for-agency"
./webdiff.sh http://sms.dev.obanyc.com:8080/onebusaway-nyc-api-webapp/api/where/vehicles-for-agency/MTABC.xml?key=TEST http://localhost:8080/onebusaway-api-webapp/api/where/vehicles-for-agency/MTABC.xml?key=TEST
