package org.citisense.service;

import java.util.Map;

@SuppressWarnings("unchecked")
public interface CitiSenseRepository {

	public int newStudy(String studyName, Map studyAttributes);

	public int newSubject(Map subjectAttributes);

	public int newDevice(Map deviceAttributes);

	public void associateSubjectToStudy(String studyID, String subjectID);

	public void associateDeviceToSubject(String deviceID, String subjectID);

	public void associateDeviceToDevice(String deviceIDFrom, String deviceIDTo);
}
