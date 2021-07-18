package org.panda.causalpath.network;

/**
 * Enumeration of the relation types used in the causality framework.
 */
public enum RelationType
{
	UPREGULATES_EXPRESSION(1, false, true, false, false, false, false),
	DOWNREGULATES_EXPRESSION(-1, false, true, false, false, false, false),
	PHOSPHORYLATES(1, true, false, false, false, false, false),
	DEPHOSPHORYLATES(-1, true, false, false, false, false, false),
	ACETYLATES(1, false, false, false, true, false, false),
	DEACETYLATES(-1, true, false, false, true, false, false),
	METHYLATES(1, false, false, false, false, true, false),
	DEMETHYLATES(-1, true, false, false, false, true, false),

	// This means source is a GEF that separates GDP from inactive GTPase protein.
	ACTIVATES_GTPASE(1, false, false, true, false, false, false),

	// This means source is a GAP that activates GTP hydrolysis function of the GTPase, which makes GTPase inactive.
	INHIBITS_GTPASE(-1, false, false, true, false, false, false),

	PRODUCES(1, false, false, false, false, false, true),
	CONSUMES(-1, false, false, false, false, false, true),
	USED_TO_PRODUCE(1, false, false, false, false, false, true),
	;

	/**
	 * Whether the relation can explain a change in phosphorylation.
	 */
	public boolean affectsPhosphoSite;

	/**
	 * Whether the relation can explain a change in total protein.
	 */
	public boolean affectsTotalProt;

	/**
	 * Whether the relation can change GTPase activity.
	 */
	public boolean affectsGTPase;

	/**
	 * Whether the relation affects acetylations.
	 */
	public boolean affectsAcetylSite;

	/**
	 * Whether the relation affects methylations.
	 */
	public boolean affectsMethlSite;

	/**
	 * Whether the relation affects methylations.
	 */
	public boolean affectsMetabolite;


	/**
	 * Sign of the relation: positive (1) or negative (-1).
	 */
	public int sign;

	RelationType(int sign, boolean affectsPhosphoSite, boolean affectsTotalProt, boolean affectsGTPase,
		boolean affectsAcetylSite, boolean affectsMethlSite, boolean affectsMetabolite)
	{
		this.sign = sign;
		this.affectsPhosphoSite = affectsPhosphoSite;
		this.affectsTotalProt = affectsTotalProt;
		this.affectsGTPase = affectsGTPase;
		this.affectsAcetylSite = affectsAcetylSite;
		this.affectsMethlSite = affectsMethlSite;
		this.affectsMetabolite = affectsMetabolite;
	}

	public String getName()
	{
		return toString().toLowerCase().replaceAll("_", "-");
	}

	public static RelationType getType(String name)
	{
		try
		{
			return valueOf(name.toUpperCase().replaceAll("-", "_"));
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public boolean isSiteSpecific()
	{
		return affectsPhosphoSite || affectsAcetylSite || affectsMethlSite;
	}
}
