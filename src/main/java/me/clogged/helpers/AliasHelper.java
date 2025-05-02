package me.clogged.helpers;

import me.clogged.data.KCAliasResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AliasHelper {
    private final Map<String, String> aliasMap = new HashMap<>();

    public AliasHelper(List<KCAliasResponse> kcAliases) {
        for (KCAliasResponse kcAlias : kcAliases) {
            aliasMap.put(kcAlias.getAlias().toLowerCase(), kcAlias.getFullName().toLowerCase());
        }
    }

    public String getFullNameByAlias(String alias) {
        if (aliasMap.containsKey(alias.toLowerCase())) {
            return aliasMap.get(alias.toLowerCase());
        }

        return alias.toLowerCase();
    }
}
