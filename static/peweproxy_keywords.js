peweproxy.register_module('keywords', function($) {

	var peweproxy_url_keywords = 'adaptive-proxy/key_words_call'
	
	
		$(document).ready(function(){
			$('div#peweproxy_icon_banner a.__peweproxy_keywords_button').click(function(){
				$(this).blur();
				renewSmallButton = false;
				$('#peweproxy_keywords').hide().removeClass('hidden').fadeIn('fast');
				$(peweproxy.modules.bubble.peweproxy_addonIconBannerSelector).addClass('hidden');
				$(peweproxy.modules.bubble.smallButtonSelector).addClass('hidden');
				get_keywords();
				return false;
			});
			$('div#peweproxy_keywords a.__peweproxy_keywords_closebutton').click(function(){
				$(this).blur();
				renewSmallButton = true;
				$(peweproxy.modules.bubble.smallButtonSelector).removeClass('hidden');
				$('#peweproxy_keywords').fadeOut('fast');
				return false;
			});
			$('form#peweproxy_keywords_add_form').submit(function(){
				post_data = $(this).serialize();
				$.post(peweproxy_url_keywords+'?action=addKeyWord',post_data,function(data){
					response = $.trim(data);
					if (response == 'OK'){
						get_keywords();
						$('input#peweproxy_keywords_add_term').val('Slovo').focus();
						$('input#peweproxy_keywords_add_type').val('Typ');
						$('input#peweproxy_keywords_add_relevance').val('Váha');
					} else {
						alert('Pridávanie sa nepodarilo, skontrolujte správnosť údajov a akciu opakujte.');
					}
				})
				return false;
			});
		});
	
	
	this.keywords_edit = function(id){
		$('div#peweproxy_keywords_content table tr.row'+id+' span.static').hide();
		$('div#peweproxy_keywords_content table tr.row'+id+' span.editable').show();
	}
	
	this.keywords_delete = function(id){
		if (confirm('Skutočne odstrániť kľúčové slovo '+$('div#peweproxy_keywords_content table tr.row'+id+' span.term').html()+'?')){
			$.post(peweproxy_url_keywords+'?action=removeKeyWord',{
				id:id
			},function(data){
				response = $.trim(data);
				if (response == 'OK'){
					get_keywords();
				} else {
					alert('Pri mazaní kľúčového slova nastala chyba.');
				}
			});
		}
	}
	
	this.keywords_save = function(id){
		table = $('div#peweproxy_keywords_content table');
		post_data = {
			id:id,
			term:table.find("tr.row"+id+" input.term").val(),
			relevance: table.find("tr.row"+id+" input.relevance").val(),
			type: table.find("tr.row"+id+" input.type").val()
		}
		$.post(peweproxy_url_keywords+'?action=editKeyWord',post_data, function(data){
			response = $.trim(data);
			if (response == 'OK'){
				get_keywords()
			} else if (response == 'TERM_EXISTS'){
				alert('Kľúčové slovo '+post_data.term+' už existuje');
			} else {
				alert('Pri ukladaní kľúčového slova nastala chyba. Skonstrolujte správnosť údajov.');
			}
		});
	}
	
	var get_keywords = function(){
		var template = '<tr class="row[:id:] keyword_row">'+
							'<td style="width: 179px">'+
								'<span class="static term">[:term:]</span>'+
								'<span class="editable"><input type="text" class="term" value="[:term:]"/></span>'+
							'</td>'+
							'<td style="width: 56px">'+
								'<span class="static type">[:type:]</span>'+
								'<span class="editable"><input type="text" class="type" value="[:type:]"/></span>'+
							'</td>'+
							'<td style="width: 43px">'+
								'<span class="static relevance">[:relevance:]</span>'+
								'<span class="editable"><input type="text" class="relevance" value="[:relevance:]"/></span>'+
							'</td>'+
							'<td style="width: 57px">[:source:]</td>'+
							'<td style="width: 46px" class="buttons">'+
								'<span class="static">'+
									'<a href="#" onclick="peweproxy.modules.keywords.keywords_edit(\'[:id:]\'); return false"><img src="'+peweproxy_keywords_htdocs_dir+'/edit_icon.png" alt="edit" /></a>'+
								'</span>'+
								'<span class="editable">'+
									'<a href="#" onclick="peweproxy.modules.keywords.keywords_save(\'[:id:]\'); return false;"><img src="'+peweproxy_keywords_htdocs_dir+'/ok_icon.png" alt="save" /></a>'+
								'</span>'+
								'<a href="#" onclick="peweproxy.modules.keywords.keywords_delete(\'[:id:]\'); return false;"><img src="'+peweproxy_keywords_htdocs_dir+'/delete_icon.png" alt="delete" /></a>'+
							'</td>'+
						'</tr>';
		$.get(peweproxy_url_keywords+'?action=getKeyWords',function(data){
			keywords = eval('('+data+')');
			table = $('div#peweproxy_keywords_content table');
			table.find("tr.keyword_row").remove();
			$.each(keywords.keywords, function() {
			    if (this.name == undefined) return;
				
				if (this.type == undefined) this.type = "";
				if (this.relevance == undefined) this.relevance = "";
				
				row = template.replace(/\[:id:\]/g, this.name);
				row = row.replace(/\[:term:\]/g, this.name);
				row = row.replace(/\[:type:\]/g, this.type);
				row = row.replace(/\[:relevance:\]/g, this.relevance);
				row = row.replace(/\[:source:\]/g, this.source);
				table.append(row);
			});
			$('div#peweproxy_keywords_content table tr:even').addClass('odd');
		})
	}

});