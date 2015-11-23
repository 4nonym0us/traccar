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
package org.traccar.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.traccar.Context;
import org.traccar.model.MiscFormatter;
import org.traccar.model.Position;

import java.util.*;

public class PositionServlet extends BaseServlet {

    @Override
    protected boolean handle(String command, HttpServletRequest req, HttpServletResponse resp) throws Exception {

        switch (command) {
            case "/filter":
                filter(req, resp);
                break;
            case "/get":
                get(req, resp);
                break;
            case "/devices":
                devices(req, resp);
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * Method is used to find downtime points for the selected period of time for a specified device
     * with a user-defined speed and downtime limits.
     * Example HTTP GET request:
     * ~/api/position/filter?deviceId=2&date_from=2015-11-13T20%3A27%3A36.000Z&date_to=2015-11-14T20%3A57%3A36.000Z&speed_from=0&speed_to=5&delay=15
     * @param req
     * @param resp
     * @throws Exception
     */
    private void filter(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        // Retrieving GET parameters
        long deviceId = Long.parseLong(req.getParameter("deviceId"));
        Date date_from = JsonConverter.parseDate(req.getParameter("date_from")),
                date_to = JsonConverter.parseDate(req.getParameter("date_to"));
        double speed_from = Double.parseDouble(req.getParameter("speed_from")),
                speed_to = Double.parseDouble(req.getParameter("speed_to"));
        int downtime_limit = Integer.parseInt(req.getParameter("delay"));

        // Fetching data from DB
        Context.getPermissionsManager().checkDevice(getUserId(req), deviceId);
        Collection<Position> temp = Context.getDataManager().getFilteredPositions(
                getUserId(req), deviceId, date_from, date_to, speed_from, speed_to);
        Position[] positions = temp.toArray(new Position[temp.size()]);

        // Detecting downtime points
        ArrayList<Position> resultCollection = new ArrayList<>();
        for (int i = 1; i < positions.length; i++) {
            // Calculating delay (in minutes) between two consecutive Positions
            int positionDelay = (int)((positions[i].getFixTime().getTime()/60000)
                    - (positions[i-1].getFixTime().getTime()/60000));

            if (positionDelay >= downtime_limit){ // True if downtime limit exceeded
                resultCollection.add(positions[i-1]);
            }
        }

        sendResponse(resp.getWriter(), JsonConverter.arrayToJson(resultCollection));
    }

    private void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        long deviceId = Long.parseLong(req.getParameter("deviceId"));
        Context.getPermissionsManager().checkDevice(getUserId(req), deviceId);
        sendResponse(resp.getWriter(), JsonConverter.arrayToJson(
                    Context.getDataManager().getPositions(
                            getUserId(req), deviceId,
                            JsonConverter.parseDate(req.getParameter("from")),
                            JsonConverter.parseDate(req.getParameter("to")))));
    }

    private void devices(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        long userId = getUserId(req);
        Map<String, Object> positions = new HashMap<>();

        for (String deviceIdString : req.getParameterValues("devicesId")) {
            Long deviceId = Long.parseLong(deviceIdString);

            Context.getPermissionsManager().checkDevice(userId, deviceId);

            Position position = Context.getConnectionManager().getLastPosition(deviceId);
            positions.put(deviceId.toString(), position);
        }

        sendResponse(resp.getWriter(), MiscFormatter.toJson(positions));
    }
}
