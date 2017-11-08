package org.panda.causalpath.resource;

import org.panda.causalpath.analyzer.CausalityHelper;
import org.panda.causalpath.analyzer.OneDataChangeDetector;
import org.panda.causalpath.data.*;
import org.panda.causalpath.network.Relation;
import org.panda.resource.tcga.ProteomicsFileRow;

import java.util.*;

/**
 * Coverts the RPPAData in resource project to appropriate objects for this project and serves them.
 */
public class ProteomicsLoader
{
	/**
	 * Map from genes to related data.
	 */
	Map<String, Set<ExperimentData>> dataMap;

	/**
	 * Set of all data. This collection supposed to hold everything in the dataMap's values.
	 */
	Set<ExperimentData> datas;

	/**
	 * Initializes self using a set of RPPAData.
	 */
	public ProteomicsLoader(Collection<ProteomicsFileRow> rows)
	{
		dataMap = new HashMap<>();
		datas = new HashSet<>();
		rows.stream().distinct().forEach(r ->
		{
			ExperimentData ed = r.isActivity() ? new ActivityData(r) :
				r.isPhospho() ? new PhosphoProteinData(r) : new ProteinData(r);

			for (String sym : ed.getGeneSymbols())
			{
				if (!dataMap.containsKey(sym)) dataMap.put(sym, new HashSet<>());
				dataMap.get(sym).add(ed);
				datas.add(ed);
			}
		});
	}

	/**
	 * Adds the related data to the given relations.
	 */
	public void decorateRelations(Set<Relation> relations)
	{
		Map<String, GeneWithData> map = collectExistingData(relations);
		CausalityHelper ch = new CausalityHelper();
		for (Relation rel : relations)
		{
			if (rel.sourceData == null)
			{
				if (map.containsKey(rel.source))
				{
					rel.sourceData = map.get(rel.source);
				}
				else
				{
					GeneWithData gwd = new GeneWithData(rel.source);
					gwd.addAll(dataMap.get(rel.source));
					map.put(gwd.getId(), gwd);
				}
			}
			else rel.sourceData.addAll(dataMap.get(rel.source));

			if (rel.targetData == null)
			{
				if (map.containsKey(rel.target))
				{
					rel.targetData = map.get(rel.target);
				}
				else
				{
					GeneWithData gwd = new GeneWithData(rel.target);
					gwd.addAll(dataMap.get(rel.target));
					map.put(gwd.getId(), gwd);
				}
			}
			else rel.targetData.addAll(dataMap.get(rel.target));

			rel.chDet = ch;
		}
	}

	private Map<String, GeneWithData> collectExistingData(Set<Relation> relations)
	{
		Map<String, GeneWithData> map = new HashMap<>();

		for (Relation rel : relations)
		{
			if (rel.sourceData != null) map.put(rel.sourceData.getId(), rel.sourceData);
			if (rel.targetData != null) map.put(rel.targetData.getId(), rel.targetData);
		}

		return map;
	}

	/**
	 * Puts the given change detector to the data that is filtered by the given selector.
	 */
	public void associateChangeDetector(OneDataChangeDetector chDet, DataSelector selector)
	{
		datas.stream().filter(selector::select).forEach(d -> d.setChDet(chDet));
	}

	/**
	 * Function to filter experiment data.
	 */
	public interface DataSelector
	{
		boolean select(ExperimentData data);
	}
}
