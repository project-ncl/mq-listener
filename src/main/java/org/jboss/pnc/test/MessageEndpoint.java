package org.jboss.pnc.test;

import org.jboss.pnc.messaging.spi.BuildStatusChanged;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/")
public class MessageEndpoint {

    @Inject
    QueueListener listener;

    @GET
    @Path("/last-message")
    public BuildStatusChanged greeting() {
        return listener.getLastMessage();
    }

    @GET
    @Path("/ready")
    public Response isReady() {
        return listener.isConnected() ? Response.ok().build() : Response.status(503).build();
    }

}
