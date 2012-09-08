/*
 * Copyright 2012 Jeanfrancois Arcand
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package dan;

import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.cpr.*;

import java.io.IOException;
import java.util.Date;

/**
 * Simple AtmosphereHandler that implement the logic to build a Chat application.
 *
 * @author Jeanfrancois Arcand
 */
@AtmosphereHandlerService(path="/chat")
public class ChatAtmosphereHandler implements AtmosphereHandler {

    @Override
    public void onRequest(AtmosphereResource r) throws IOException {

        AtmosphereRequest req = r.getRequest();
        String c = req.getParameter("c"); //channel
        if (c != null) {
            Broadcaster dbc = r.getBroadcaster();
            dbc.removeAtmosphereResource(r);
            Broadcaster bc = BroadcasterFactory.getDefault().lookup(c, true);
            r.setBroadcaster(bc);
            bc.addAtmosphereResource(r);
        }
        String p = req.getParameter("w");
        if (p == null) {
            String m = req.getParameter("m");
            r.getBroadcaster().broadcast(m);
            r.getResponse().getWriter().write("ok");
        } else {

            r.suspend();
        }

//        // First, tell Atmosphere to allow bi-directional communication by suspending.
//        if (req.getMethod().equalsIgnoreCase("GET")) {
//            // The negotiation header is just needed by the sample to list all the supported transport.
//            if (req.getHeader("negotiating") == null) {
//                r.suspend();
//            } else {
//                r.getResponse().getWriter().write("OK");
//            }
//        // Second, broadcast message to all connected users.
//        } else if (req.getMethod().equalsIgnoreCase("POST")) {
//            r.getBroadcaster().broadcast(req.getReader().readLine().trim());
//        }
    }

    @Override
    public void onStateChange(AtmosphereResourceEvent event) throws IOException {
        AtmosphereResource r = event.getResource();
        AtmosphereResponse res = r.getResponse();

        if (event.isSuspended()) {
            String body = event.getMessage().toString();
            res.getWriter().write(body);
            switch (r.transport()) {
                case JSONP:
                case AJAX:
                case LONG_POLLING:
                    event.getResource().resume();
                    break;
                default:
                    event.getResource().resume();
//                    res.getWriter().flush();
                    break;
            }
        } else if (!event.isResuming()){
           // event.broadcaster().broadcast(new Data("Someone", "say bye bye!").toString());
        }
    }

    @Override
    public void destroy() {
    }

    private final static class Data {

        private final String text;
        private final String author;

        public Data(String author, String text) {
            this.author = author;
            this.text = text;
        }

        public String toString() {
            return "{ \"text\" : \"" + text + "\", \"author\" : \"" + author + "\" , \"time\" : " + new Date().getTime() + "}";
        }
    }
}
