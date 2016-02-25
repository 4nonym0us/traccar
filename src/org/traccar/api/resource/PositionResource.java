/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.api.resource;

import org.traccar.Context;
import org.traccar.api.BaseResource;
import org.traccar.model.Position;
import org.traccar.web.JsonConverter;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

@Path("positions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PositionResource extends BaseResource {

    @GET
    public Collection<Position> get(
            @QueryParam("deviceId") long deviceId, @QueryParam("from") String from, @QueryParam("to") String to)
            throws SQLException {
        Context.getPermissionsManager().checkDevice(getUserId(), deviceId);
        return Context.getDataManager().getPositions(
                deviceId, JsonConverter.parseDate(from), JsonConverter.parseDate(to));
    }

    @Path("filter")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Collection<Position> filter(
            @QueryParam("deviceId") long deviceId, @QueryParam("date_from") String dateFromStr, @QueryParam("date_to") String dateToStr,
            @QueryParam("speed_from") double speedFrom, @QueryParam("speed_to") double speedTo, @QueryParam("delay") int downtimeLimit)
            throws SQLException {

        Date dateFrom = JsonConverter.parseDate(dateFromStr),
                dateTo = JsonConverter.parseDate(dateToStr);

        // Fetching data from DB
        Context.getPermissionsManager().checkDevice(getUserId(), deviceId);
        Collection<Position> temp = Context.getDataManager()
                .getFilteredPositions(deviceId, dateFrom, dateTo, speedFrom, speedTo);
        Position[] positions = temp.toArray(new Position[temp.size()]);

        // Detecting downtime points
        Collection<Position> resultCollection = new ArrayList<>();
        for (int i = 1; i < positions.length; i++) {
            // Calculating delay (in minutes) between two consecutive Positions
            int positionDelay = (int)((positions[i].getFixTime().getTime()/60000)
                    - (positions[i-1].getFixTime().getTime()/60000));

            if (positionDelay >= downtimeLimit){ // True if downtime limit exceeded
                resultCollection.add(positions[i-1]);
            }
        }
        return resultCollection;
    }

}
