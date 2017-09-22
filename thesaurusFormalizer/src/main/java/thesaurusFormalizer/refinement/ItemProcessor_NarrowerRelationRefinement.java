package thesaurusFormalizer.refinement;

import org.springframework.batch.item.ItemProcessor;

import org.apache.jena.rdf.model.Resource;

/**
 * refina las relaciones nt reemplazandolas por otras más precisas proporcionadas
 * por dolce
 */
public class ItemProcessor_NarrowerRelationRefinement implements ItemProcessor<Resource, Resource>{
	/**
	 * refina las relaciones nt reemplazandolas por otras más precisas proporcionadas
	 * por dolce (pendiente por hacer, no se hizo en la version 1.0 fue a mano)
	 */
	public Resource process(Resource item) throws Exception {
		return item;
	}
}
