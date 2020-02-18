package org.zendesk.client.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class IncrementalResults<A,B,C> {

	@JsonProperty("next_page")
	private String nextPage;

	@JsonProperty("end_of_stream")
	private Boolean endOfStream;

	@JsonProperty("end_time")
	private Long endTime;

	public String getNextPage() {
		return nextPage;
	}

	public void setNextPage(String nextPage) {
		this.nextPage = nextPage;
	}

	public Boolean getEndOfStream() {
		return endOfStream;
	}

	public void setEndOfStream(Boolean endOfStream) {
		this.endOfStream = endOfStream;
	}

	public Long getEndTime() {
		return endTime;
	}

	public void setEndTime(Long endTime) {
		this.endTime = endTime;
	}
}
