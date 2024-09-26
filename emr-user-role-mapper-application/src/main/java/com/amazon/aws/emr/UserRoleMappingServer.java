// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.aws.emr;

import com.amazon.aws.emr.common.Constants;
import com.amazon.aws.emr.ws.UserRoleMapperApplication;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.Scheduler;
import org.glassfish.jersey.servlet.ServletContainer;

import java.util.concurrent.Executor;

/**
 * Server that handles all user role mapping requests.
 */
@Slf4j
public class UserRoleMappingServer {

    public static void main(String[] args) {

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");

        ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();
        applicationConfiguration.init();
        int maxThreads = applicationConfiguration.getProperty(Constants.ROLE_MAPPING_MAX_THREADS, Constants.ROLE_MAPPING_DEFAULT_MAX_THREADS);
        int minThreads = applicationConfiguration.getProperty(Constants.ROLE_MAPPING_MIN_THREADS, Constants.ROLE_MAPPING_DEFAULT_MIN_THREADS);
        log.info("Starting with max {} and min {} threads", maxThreads, minThreads);

        QueuedThreadPool pool = new QueuedThreadPool();
        pool.setMaxThreads(maxThreads);
        pool.setMinThreads(minThreads);
        pool.setIdleTimeout(Constants.ROLE_MAPPING_DEFAULT_IDLE_TIMEOUT_MS);
        pool.setName("worker-thread");

        Server jettyServer = new Server(pool);
        jettyServer.setHandler(context);

        /*
        ServerConnector httpConnector = new ServerConnector(jettyServer);
        httpConnector.setPort(Constants.JETTY_PORT);
        jettyServer.addConnector(httpConnector);
        */
        // Setup SSL
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath("server-key-store");
        sslContextFactory.setKeyStorePassword("secret");
        sslContextFactory.setKeyManagerPassword("secret");
        sslContextFactory.setTrustStorePath("server-trust-store");
        sslContextFactory.setTrustStorePassword("secret");
        sslContextFactory.setWantClientAuth(true);
        sslContextFactory.setNeedClientAuth(true);

        ServerConnector httpsConnector = new ServerConnector(jettyServer, sslContextFactory);
        httpsConnector.setPort(9943); //TODO: figure out the https port

        jettyServer.addConnector(httpsConnector);

        ServletHolder jerseyServlet = context.addServlet(ServletContainer.class, "/*");
        jerseyServlet.setInitOrder(0);

        // Tells the Jersey Servlet which REST service/class to load.
        jerseyServlet.setInitParameter("jersey.config.server.provider.packages", "com.amazon.emr.api");
        jerseyServlet.setInitParameter("javax.ws.rs.Application", UserRoleMapperApplication.class.getName());

        try {
            log.info("Starting the user role mapping server");
            jettyServer.start();
            jettyServer.join();
        } catch (Exception e) {
            log.error("Error in user role mapping server", e);
        } finally {
            jettyServer.destroy();
        }
    }

    }
