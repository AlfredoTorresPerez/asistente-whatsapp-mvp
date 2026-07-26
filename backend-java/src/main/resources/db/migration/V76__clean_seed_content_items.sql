-- V76: Limpia registro corrupto de contenido visual (data de prueba que quedo pegada)
delete from content_items where id = '70000000-0000-0000-0000-000000000001'
  and text like 'ssss%';
