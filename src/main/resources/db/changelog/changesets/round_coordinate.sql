UPDATE substation_entity
SET latitude = ROUND(CAST(latitude as numeric), 5), longitude = ROUND(CAST(longitude as numeric), 5);
UPDATE line_entity
SET coordinates = (
    SELECT jsonb_agg(
                   jsonb_set(
                           jsonb_set(elem, '{lat}', to_jsonb(round((elem->>'lat')::numeric, 5))),
                           '{lon}', to_jsonb(round((elem->>'lon')::numeric, 5)))
           )
    FROM jsonb_array_elements(coordinates::jsonb) AS elem
);