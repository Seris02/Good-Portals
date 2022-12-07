package com.seris02.goodportals.storage;

public interface StorageEmittee {
	public void fullUpdate();
	public void updateSpecific(DataStorage.Var type, DataStorage data);
}
