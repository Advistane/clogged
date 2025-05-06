package me.clogged.data;

import lombok.Getter;
import java.util.List;

@Getter
public class CollectionLogLookupResponse {
    private int kc;
    private String subcategoryName;
    private int total;
    private List<CollectionLogItem> items;
}