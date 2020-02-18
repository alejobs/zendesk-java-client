package org.zendesk.client.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class IncrementalResultsTUM extends IncrementalResults {

	@JsonProperty("tickets")
	private List<Ticket> tickets;

	@JsonProperty("users")
	private List<User> users;

	@JsonProperty("metric_sets")
	private List<Metric> metricSets;

	public List<Ticket> getTickets() {
		return tickets;
	}

	public void setTickets(List<Ticket> tickets) {
		this.tickets = tickets;
	}

	public List<User> getUsers() {
		return users;
	}

	public void setUsers(List<User> users) {
		this.users = users;
	}

	public List<Metric> getMetricSets() {
		return metricSets;
	}

	public void setMetricSets(List<Metric> metricSets) {
		this.metricSets = metricSets;
	}
}
