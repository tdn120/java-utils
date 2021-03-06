package net.thomasnardone.ui.rest;

import java.util.List;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;

public class AutoTableClient extends TableClient {
	private final Client	client;
	private final String	host;
	private final String	servletName;

	/**
	 * Create a client for a table's corresponding service.
	 * 
	 * @param host
	 *            Hostname, with port if needed. Must include schema (e.g. http:// or https://)
	 * @param servletName
	 *            The servlet's name, typically the name of the war file.
	 */
	public AutoTableClient(final String host, final String servletName, final String username, final String password) {
		this.host = host;
		this.servletName = servletName;
		ClientConfig clientConfig = new DefaultClientConfig();
		clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
		client = Client.create(clientConfig);
		client.addFilter(new HTTPBasicAuthFilter(username, password));
	}

	/**
	 * Get the table data from the given service.
	 * 
	 * @param serviceName
	 *            The service name, set with an <code>@Path()</code> definition on the service class.
	 */
	@Override
	public String[][] getData(final String serviceName) {
		return getResource(serviceName, "data").get(String[][].class);
	}

	/**
	 * Get the table info - columns, filters, and formats
	 * 
	 * @param serviceName
	 *            The service name, set with an <code>@Path()</code> definition on the service class.
	 */
	@Override
	public TableInfo getTableInfo(final String serviceName) {
		return getResource(serviceName, "info").get(TableInfo.class);
	}

	/**
	 * Update the table data on the server.
	 * 
	 * @param serviceName
	 *            The service name, set with an <code>@Path()</code> definition on the service class.
	 */
	@Override
	public boolean updateTable(final String serviceName, final List<UpdateInfo> update) {
		return getResource(serviceName, "update").post(Boolean.class, update);
	}

	private WebResource.Builder getResource(final String serviceName, final String function) {
		return client.resource(host).path(servletName).path(serviceName).path(function).type(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);
	}
}
