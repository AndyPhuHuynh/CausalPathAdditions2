package org.panda.causalpath.network;

import org.panda.causalpath.analyzer.TwoDataChangeDetector;
import org.panda.causalpath.data.ExperimentData;
import org.panda.causalpath.data.GeneWithData;
import org.panda.causalpath.data.ProteinSite;
import org.panda.utility.CollectionUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * That is a potential causality relation.
 */
public class Relation
{
	/**
	 * Source gene.
	 */
	public String source;

	/**
	 * Target gene.
	 */
	public String target;

	/**
	 * The type of the relation.
	 */
	public RelationType type;

	/**
	 * Set of experiment data associated with source gene.
	 */
	public GeneWithData sourceData;

	/**
	 * Set of experiment data associated with target gene.
	 */
	public GeneWithData targetData;

	/**
	 * Pathway Commons IDs of the mediator objects for the relation.
	 */
	private String mediators;

	/**
	 * A change detector that can evaluate pairs of experiment data to see if this relation can explain the causality
	 * between them.
	 */
	public TwoDataChangeDetector chDet;

	/**
	 * Sites for the target. Needed when the relation is a phosphorylation or dephosphorylation.
	 */
	public Set<ProteinSite> sites;

	/**
	 * For performance reasons. This design assumes the proximityThreshold will not change during execution of the
	 * program.
	 */
	private Set<String> targetWithSites;

	public Relation(String source, String target, RelationType type, String mediators)
	{
		this.source = source;
		this.target = target;
		this.type = type;
		this.mediators = mediators;
	}

	public Relation(String line)
	{
		String[] token = line.split("\t");
		this.source = token[0];
		this.target = token[2];
		this.type = RelationType.getType(token[1]);
		if (token.length > 3) this.mediators = token[3];
		if (token.length > 4)
		{
			sites = Arrays.stream(token[4].split(";")).map(s -> new ProteinSite(Integer.valueOf(s.substring(1)),
				String.valueOf(s.charAt(0)), 0)).collect(Collectors.toSet());
		}
	}

	/**
	 * Sign of the relation.
	 */
	public int getSign()
	{
		return type.sign;
	}

	public String toString()
	{
		return source + "\t" + type.getName() + "\t" + target + "\t" + mediators + "\t" + getSitesInString();
	}

	public String getMediators()
	{
		return mediators;
	}

	public Set<String> getTargetWithSites(int proximityThr)
	{
		if (targetWithSites == null)
		{
			targetWithSites = new HashSet<>();

			if (sites != null)
			{
				for (ProteinSite site : sites)
				{
					for (int i = 0; i <= proximityThr; i++)
					{
						targetWithSites.add(target + "_" + (site.getSite() + i));
						targetWithSites.add(target + "_" + (site.getSite() - i));
					}
				}
			}
		}

		return targetWithSites;
	}

	public String getSitesInString()
	{
		return sites == null ? "" : CollectionUtil.merge(sites, ";");
	}

	public Set<ExperimentData> getAllData()
	{
		return CollectionUtil.getUnion(sourceData.getData(), targetData.getData());
	}

	public void setChDet(TwoDataChangeDetector chDet)
	{
		this.chDet = chDet;
	}

	@Override
	public int hashCode()
	{
		return source.hashCode() + target.hashCode() + type.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof Relation && ((Relation) obj).source.equals(source) &&
			((Relation) obj).target.equals(target) && ((Relation) obj).type.equals(type);
	}

	public Relation copy()
	{
		Relation cln = new Relation(source, target, type, mediators);
		cln.sites = sites;
		return cln;
	}
}
