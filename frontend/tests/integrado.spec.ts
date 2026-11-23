import {test,expect} from '@playwright/test';
import {resolve} from 'node:path';
import {randomUUID} from 'node:crypto';

test('candidato e recrutador percorrem o mesmo processo na API Java',async({page,browser})=>{
  test.skip(!process.env.TEST_RECRUTADOR_EMAIL,'Requer a instalação descartável de scripts/demonstrar.py');
  const name='Pessoa '+randomUUID().slice(0,8);
  const email=randomUUID()+'@example.invalid';
  await page.goto('/');
  await page.getByRole('button',{name:'Ver vaga'}).first().click();
  await page.getByRole('button',{name:'Entrar para se candidatar'}).click();
  await page.getByRole('button',{name:'Criar conta de candidato'}).click();
  await page.getByLabel('Nome completo').fill(name);
  await page.getByLabel('E-mail',{exact:true}).fill(email);
  await page.getByLabel('Senha',{exact:true}).fill(randomUUID());
  await page.getByRole('button',{name:'Criar conta de candidato'}).click();
  await page.getByLabel('Currículo em PDF').setInputFiles(resolve('../scripts/fixtures/curriculo-demo.pdf'));
  await page.getByRole('button',{name:'Enviar candidatura'}).click();
  await expect(page.getByRole('button',{name:/Desenvolvedor Java/})).toContainText('Triagem');

  const recruiter=await browser.newContext({baseURL:process.env.TEST_BASE_URL});
  try{
    const panel=await recruiter.newPage();
    await panel.goto('/');
    await panel.getByRole('button',{name:'Entrar',exact:true}).click();
    await panel.getByLabel('E-mail',{exact:true}).fill(process.env.TEST_RECRUTADOR_EMAIL!);
    await panel.getByLabel('Senha',{exact:true}).fill(process.env.TEST_RECRUTADOR_SENHA!);
    await panel.getByRole('dialog').getByRole('button',{name:'Entrar',exact:true}).click();
    await panel.getByRole('button',{name:new RegExp(name)}).click();
    const file=panel.waitForEvent('download');
    await panel.getByRole('button',{name:'Baixar currículo'}).click();
    expect((await file).suggestedFilename()).toBe('curriculo.pdf');
    await panel.getByLabel('Alterar etapa').selectOption('ENTREVISTA');
    await panel.getByRole('button',{name:'Salvar etapa'}).click();
    await expect(panel.getByRole('listitem').last()).toContainText('Entrevista');
    await page.getByRole('button',{name:/Desenvolvedor Java/}).click();
    await expect(page.getByRole('listitem').last()).toContainText('Entrevista');
  }finally{await recruiter.close()}
});
