package org.panda.causalpath.data;

import org.panda.resource.tcga.ProteomicsFileRow;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Data specific to protein phosphorylation.
 */
public class PhosphoProteinData extends ProteinData
{
	/**
	 * Map from gene symbols to corresponding sites on proteins.
	 */
	protected Map<String, Set<PhosphoSite>> siteMap;

	public PhosphoProteinData(String id, Set<String> geneSymbols)
	{
		super(id, geneSymbols);
	}

	/**
	 * Constructor to convert an RPPAData object from the "resource" project.
	 */
	public PhosphoProteinData(ProteomicsFileRow row)
	{
		super(row);

		siteMap = new HashMap<>();

		row.sites.keySet().stream().forEach(sym ->
			siteMap.put(sym, row.sites.get(sym).stream().map(site ->
				new PhosphoSite(Integer.parseInt(site.substring(1)), site.substring(0, 1),
					row.effect == null ? 0 : row.effect == ProteomicsFileRow.SiteEffect.ACTIVATING ? 1 :
					row.effect == ProteomicsFileRow.SiteEffect.INHIBITING ? -1 : 0)).collect(Collectors.toSet())));
	}

	/**
	 * Effect of this data depends on the effect of the phospho site.
	 */
	@Override
	public int getEffect()
	{
		boolean activating = siteMap.values().stream().flatMap(Collection::stream).anyMatch(site -> site.effect > 0);
		boolean inhibiting = siteMap.values().stream().flatMap(Collection::stream).anyMatch(site -> site.effect < 0);

		if (activating && !inhibiting) return 1;
		if (!activating && inhibiting) return -1;
		return 0;
	}

	/**
	 * Gets string that shows genes with their sites in a set.
	 */
	public Set<String> getGenesWithSites()
	{
		if (siteMap == null) return Collections.emptySet();

		Set<String> set = new HashSet<>();
		siteMap.keySet().stream().forEach(gene ->
			set.addAll(siteMap.get(gene).stream().map(site -> gene + "_" + site.getSite()).collect(Collectors.toList())));

		return set;
	}
}
